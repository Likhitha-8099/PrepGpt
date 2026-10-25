package com.prepgpt.service;

import com.prepgpt.dto.RunCodeResultItemDto;
import com.prepgpt.entity.CodingTestCase;
import com.prepgpt.entity.ExecutionStatus;
import com.prepgpt.exception.CodeExecutionUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@Service
public class CodeExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(CodeExecutionService.class);

    private static final String DOCKER_IMAGE = "prepgpt-sandbox:1.0.0";
    private static final long PLATFORM_MAX_TIMEOUT_MS = 5000L;
    private static final int MAX_OUTPUT_BYTES = 64 * 1024; // 64 KB limit

    public static class ExecutionBatchResult {
        public ExecutionStatus status;
        public int passedCount;
        public int totalCount;
        public long executionTimeMs;
        public String compileOutput;
        public List<RunCodeResultItemDto> testResults;

        public ExecutionBatchResult(ExecutionStatus status, int passedCount, int totalCount,
                                    long executionTimeMs, String compileOutput,
                                    List<RunCodeResultItemDto> testResults) {
            this.status = status;
            this.passedCount = passedCount;
            this.totalCount = totalCount;
            this.executionTimeMs = executionTimeMs;
            this.compileOutput = compileOutput;
            this.testResults = testResults;
        }
    }

    /**
     * Checks whether Docker daemon is active and responsive.
     */
    public boolean isDockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "info").start();
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Executes candidate code against a list of test cases exclusively inside a Docker sandbox container.
     */
    public ExecutionBatchResult executeCode(String language, String code, List<CodingTestCase> testCases, Integer problemTimeLimitMs) {
        if (!isDockerAvailable()) {
            throw new CodeExecutionUnavailableException("Code execution service is currently unavailable. Docker sandbox container runtime required.");
        }

        long timeLimitMs = problemTimeLimitMs != null ? Math.min(problemTimeLimitMs, PLATFORM_MAX_TIMEOUT_MS) : 3000L;
        String langUpper = language != null ? language.toUpperCase().trim() : "JAVA";

        if (!langUpper.equals("JAVA") && !langUpper.equals("PYTHON")) {
            throw new IllegalArgumentException("Unsupported language: " + language + ". Supported languages: JAVA, PYTHON");
        }

        long startTime = System.currentTimeMillis();
        List<RunCodeResultItemDto> results = new ArrayList<>();
        int passedCount = 0;
        String globalCompileOutput = "";

        for (CodingTestCase tc : testCases) {
            SingleTestResult str = runTestCaseInDocker(langUpper, code, tc.getInputData(), tc.getExpectedOutput(), timeLimitMs);
            if (str.status == ExecutionStatus.COMPILE_ERROR) {
                globalCompileOutput = str.output;
                results.add(new RunCodeResultItemDto(tc.getId(), false, tc.getIsHidden(), tc.getInputData(), tc.getExpectedOutput(), "", str.output));
                return new ExecutionBatchResult(ExecutionStatus.COMPILE_ERROR, 0, testCases.size(), System.currentTimeMillis() - startTime, globalCompileOutput, results);
            }

            boolean passed = str.status == ExecutionStatus.PASSED;
            if (passed) {
                passedCount++;
            }

            results.add(new RunCodeResultItemDto(
                    tc.getId(),
                    passed,
                    tc.getIsHidden(),
                    tc.getInputData(),
                    tc.getExpectedOutput(),
                    str.output,
                    str.error
            ));

            if (str.status == ExecutionStatus.TIME_LIMIT_EXCEEDED) {
                return new ExecutionBatchResult(ExecutionStatus.TIME_LIMIT_EXCEEDED, passedCount, testCases.size(), System.currentTimeMillis() - startTime, globalCompileOutput, results);
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        ExecutionStatus overallStatus = (passedCount == testCases.size()) ? ExecutionStatus.PASSED : ExecutionStatus.FAILED;
        return new ExecutionBatchResult(overallStatus, passedCount, testCases.size(), totalTime, globalCompileOutput, results);
    }

    private static class SingleTestResult {
        ExecutionStatus status;
        String output;
        String error;

        SingleTestResult(ExecutionStatus status, String output, String error) {
            this.status = status;
            this.output = output;
            this.error = error;
        }
    }

    /**
     * Launches an isolated Docker container for a single test case.
     * Container runs with strict security flags: --network=none, --memory=256m, --cpus=1.0, --pids-limit=64,
     * --user=10001:10001, --cap-drop=ALL, --security-opt=no-new-privileges, --read-only, --tmpfs /tmp:rw,noexec,nosuid,size=64m.
     */
    private SingleTestResult runTestCaseInDocker(String language, String code, String inputData, String expectedOutput, long timeLimitMs) {
        String containerName = "prepgpt-exec-" + UUID.randomUUID().toString().substring(0, 8);
        List<String> dockerCmd = buildDockerRunCommand(containerName, language);

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(dockerCmd);
            // Clear environment to prevent leaking Spring secrets
            pb.environment().clear();
            pb.environment().put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin");
            process = pb.start();

            // Feed code and inputData via stdin payload
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
                writer.write("---CODE_START---\n");
                writer.write(code);
                writer.write("\n---CODE_END---\n");
                writer.write("---INPUT_START---\n");
                writer.write(inputData != null ? inputData : "");
                writer.write("\n---INPUT_END---\n");
                writer.flush();
            }

            boolean finished = process.waitFor(timeLimitMs + 1000, TimeUnit.MILLISECONDS);
            if (!finished) {
                forceKillContainer(containerName);
                return new SingleTestResult(ExecutionStatus.TIME_LIMIT_EXCEEDED, "", "Time Limit Exceeded (" + timeLimitMs + "ms)");
            }

            String stdOut = readStream(process.getInputStream());
            String stdErr = readStream(process.getErrorStream());

            if (process.exitValue() != 0) {
                if (stdErr.contains("CompileError") || stdOut.contains("CompileError") || stdErr.contains("error:") || stdErr.contains("SyntaxError")) {
                    return new SingleTestResult(ExecutionStatus.COMPILE_ERROR, truncateOutput(stdOut), truncateOutput(stdErr));
                }
                return new SingleTestResult(ExecutionStatus.RUNTIME_ERROR, truncateOutput(stdOut), truncateOutput(stdErr));
            }

            String actualOutput = normalizeOutput(stdOut);
            String expected = normalizeOutput(expectedOutput);

            if (actualOutput.equals(expected)) {
                return new SingleTestResult(ExecutionStatus.PASSED, actualOutput, null);
            } else {
                return new SingleTestResult(ExecutionStatus.FAILED, actualOutput, "Output mismatch. Expected: [" + expected + "], Got: [" + actualOutput + "]");
            }

        } catch (Exception e) {
            logger.error("Failed to execute code in Docker sandbox container: {}", e.getMessage());
            return new SingleTestResult(ExecutionStatus.SYSTEM_ERROR, "", "Sandbox error: " + e.getMessage());
        } finally {
            forceKillContainer(containerName);
        }
    }

    private List<String> buildDockerRunCommand(String containerName, String language) {
        List<String> cmd = new ArrayList<>();
        cmd.add("docker");
        cmd.add("run");
        cmd.add("--name=" + containerName);
        cmd.add("--rm");

        // Enforced Security Flags
        cmd.add("--network=none");
        cmd.add("--memory=256m");
        cmd.add("--memory-swap=256m");
        cmd.add("--cpus=1.0");
        cmd.add("--pids-limit=64");
        cmd.add("--user=10001:10001");
        cmd.add("--cap-drop=ALL");
        cmd.add("--security-opt=no-new-privileges");
        cmd.add("--read-only");
        cmd.add("--tmpfs");
        cmd.add("/tmp:rw,noexec,nosuid,size=64m");
        cmd.add("-i"); // Interactive stdin reading
        cmd.add(DOCKER_IMAGE);
        cmd.add(language); // Passed as argument to image entrypoint script

        return cmd;
    }

    private void forceKillContainer(String containerName) {
        try {
            new ProcessBuilder("docker", "rm", "-f", containerName).start().waitFor(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private String readStream(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        int totalRead = 0;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            totalRead += nRead;
            if (totalRead <= MAX_OUTPUT_BYTES) {
                buffer.write(data, 0, nRead);
            }
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private String truncateOutput(String text) {
        if (text == null) return "";
        if (text.length() > MAX_OUTPUT_BYTES) {
            return text.substring(0, MAX_OUTPUT_BYTES) + "\n...[Output Truncated]";
        }
        return text;
    }

    private String normalizeOutput(String text) {
        if (text == null) return "";
        return text.replace("\r\n", "\n").trim();
    }
}
