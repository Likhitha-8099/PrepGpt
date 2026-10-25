package com.prepgpt.service;

import com.prepgpt.exception.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Phase 13: Resume & Document Parser Security Tests")
class ResumeSecurityTest {

    @TempDir
    Path tempUploadDir;

    private ResumeParserService parserService;

    @BeforeEach
    void setUp() {
        parserService = new ResumeParserService(tempUploadDir.toString());
    }

    @AfterEach
    void tearDown() {
        parserService.shutdown();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. GENUINE VALID DOCUMENT FIXTURES
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Genuine PDF: Parseable valid PDF byte structure extracts text successfully")
    void storeAndExtractText_validPdf_success() {
        byte[] pdfBytes = createValidPdfBytes("John Doe Software Engineer");
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", pdfBytes);

        ResumeParserService.FileStorageResult result = parserService.storeAndExtractText(file);

        assertThat(result).isNotNull();
        assertThat(result.getExtractedText()).contains("John Doe");
        assertThat(result.getFileType()).isEqualTo("PDF");
    }

    @Test
    @DisplayName("Genuine DOCX: Parseable OOXML zip container extracts text successfully")
    void storeAndExtractText_validDocx_success() throws IOException {
        byte[] docxBytes = createValidDocxBytes("Alice Smith Senior Backend Developer");
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes);

        ResumeParserService.FileStorageResult result = parserService.storeAndExtractText(file);

        assertThat(result).isNotNull();
        assertThat(result.getExtractedText()).contains("Alice Smith");
        assertThat(result.getFileType()).isEqualTo("DOCX");
    }

    @Test
    @DisplayName("Genuine TXT: UTF-8 plain text file extracts text successfully")
    void storeAndExtractText_validTxt_success() {
        byte[] txtBytes = "Bob Johnson - Full Stack Engineer with Java, React and MySQL.".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", txtBytes);

        ResumeParserService.FileStorageResult result = parserService.storeAndExtractText(file);

        assertThat(result).isNotNull();
        assertThat(result.getExtractedText()).contains("Bob Johnson");
        assertThat(result.getFileType()).isEqualTo("TXT");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. MIME SPOOFING & EXTENSION MISMATCH DEFENSE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("MIME Spoofing: Plain text renamed to .pdf is detected as text/plain and rejected")
    void storeAndExtractText_textRenamedToPdf_rejected() {
        byte[] textBytes = "This is a plain text file pretending to be a PDF".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake_resume.pdf", "application/pdf", textBytes);

        assertThatThrownBy(() -> parserService.storeAndExtractText(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid PDF document content");
    }

    @Test
    @DisplayName("MIME Spoofing: Plain text renamed to .docx is detected as text/plain and rejected")
    void storeAndExtractText_textRenamedToDocx_rejected() {
        byte[] textBytes = "This is a plain text file pretending to be a DOCX".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake_resume.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", textBytes);

        assertThatThrownBy(() -> parserService.storeAndExtractText(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid DOCX document content");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. EXECUTABLE & BINARY ATTACK DEFENSE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Executable Defense: Disguised Linux ELF binary header renamed to .pdf is rejected")
    void storeAndExtractText_elfBinaryRenamedToPdf_rejected() {
        byte[] elfBytes = new byte[]{0x7f, 'E', 'L', 'F', 0x02, 0x01, 0x01, 0x00, 0, 0, 0, 0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile(
                "file", "malicious_script.pdf", "application/pdf", elfBytes);

        assertThatThrownBy(() -> parserService.storeAndExtractText(file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Executable Defense: Disguised Windows PE executable (MZ) renamed to .docx is rejected")
    void storeAndExtractText_peBinaryRenamedToDocx_rejected() {
        byte[] peBytes = new byte[]{'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile(
                "file", "malicious.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", peBytes);

        assertThatThrownBy(() -> parserService.storeAndExtractText(file))
                .isInstanceOf(BadRequestException.class);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. MALFORMED / BOUNDARY CONDITIONS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Boundary: Empty file is rejected immediately with 400 Bad Request")
    void storeAndExtractText_emptyFile_rejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> parserService.storeAndExtractText(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    @DisplayName("Boundary: Unsupported extension (.exe, .zip, .png) is rejected")
    void storeAndExtractText_unsupportedExtension_rejected() {
        byte[] content = "Hello world".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "archive.zip", "application/zip", content);

        assertThatThrownBy(() -> parserService.storeAndExtractText(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unsupported resume file format");
    }

    @Test
    @DisplayName("Boundary: Oversized file exceeding 5MB limit is rejected")
    void storeAndExtractText_oversizedFile_rejected() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", largeBytes);

        assertThatThrownBy(() -> parserService.storeAndExtractText(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    @DisplayName("Boundary: Extracted text exceeding 100,000 characters is rejected")
    void storeAndExtractText_oversizedExtractedText_rejected() {
        // Create a TXT file with 105,000 characters
        StringBuilder sb = new StringBuilder(105_000);
        for (int i = 0; i < 105_000; i++) {
            sb.append('A');
        }
        byte[] txtBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file", "huge_text.txt", "text/plain", txtBytes);

        assertThatThrownBy(() -> parserService.storeAndExtractText(file))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("100,000 characters");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER FIXTURE GENERATORS
    // ─────────────────────────────────────────────────────────────────────────

    private byte[] createValidPdfBytes(String text) {
        String pdf = "%PDF-1.4\n" +
                "1 0 obj\n" +
                "<< /Type /Catalog /Pages 2 0 R >>\n" +
                "endobj\n" +
                "2 0 obj\n" +
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n" +
                "endobj\n" +
                "3 0 obj\n" +
                "<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 4 0 R >> >> /MediaBox [0 0 612 792] /Contents 5 0 R >>\n" +
                "endobj\n" +
                "4 0 obj\n" +
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\n" +
                "endobj\n" +
                "5 0 obj\n" +
                "<< /Length " + (text.length() + 35) + " >>\n" +
                "stream\n" +
                "BT\n" +
                "/F1 12 Tf\n" +
                "100 700 Td\n" +
                "(" + text + ") Tj\n" +
                "ET\n" +
                "endstream\n" +
                "endobj\n" +
                "xref\n" +
                "0 6\n" +
                "0000000000 65535 f \n" +
                "0000000009 00000 n \n" +
                "0000000068 00000 n \n" +
                "0000000125 00000 n \n" +
                "0000000244 00000 n \n" +
                "0000000318 00000 n \n" +
                "trailer\n" +
                "<< /Size 6 /Root 1 0 R >>\n" +
                "startxref\n" +
                "450\n" +
                "%%EOF\n";
        return pdf.getBytes(StandardCharsets.US_ASCII);
    }

    private byte[] createValidDocxBytes(String text) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // [Content_Types].xml
            zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
            String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                    "  <Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                    "  <Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
                    "  <Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>\n" +
                    "</Types>";
            zos.write(contentTypes.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // _rels/.rels
            zos.putNextEntry(new ZipEntry("_rels/.rels"));
            String rels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                    "  <Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>\n" +
                    "</Relationships>";
            zos.write(rels.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // word/document.xml
            zos.putNextEntry(new ZipEntry("word/document.xml"));
            String docXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n" +
                    "  <w:body>\n" +
                    "    <w:p>\n" +
                    "      <w:r>\n" +
                    "        <w:t>" + text + "</w:t>\n" +
                    "      </w:r>\n" +
                    "    </w:p>\n" +
                    "  </w:body>\n" +
                    "</w:document>";
            zos.write(docXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
