package com.prepgpt.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prepgpt.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe, bounded in-memory rate limiting filter.
 * Distinguishes endpoints into specific rate limit categories:
 * - Auth (Login/Register): 10 req/min (keyed by IP)
 * - AI Question Generation & Analysis: 20 req/min (keyed by User)
 * - AI Answer Evaluation: 20 req/min (keyed by User)
 * - AI Follow-up: 20 req/min (keyed by User)
 * - STT Voice Transcription: 15 req/min (keyed by User)
 * - TTS Voice Synthesis: 25 req/min (keyed by User)
 * - Coding Sandbox Execution: 15 req/min (keyed by User)
 * - General fallback: 120 req/min (keyed by User/IP)
 *
 * Enforces a strict maximum of 10,000 keys with LRU/stale eviction to prevent memory leaks.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final int MAX_ENTRIES = 10000;
    private static final long STALE_THRESHOLD_MS = 120000L; // 2 minutes

    @Value("${ratelimit.enabled:true}")
    private boolean rateLimitEnabled = true;

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(ObjectMapper objectMapper) {
        ObjectMapper mapper = objectMapper != null ? objectMapper.copy() : new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Determine rate limit category and capacity
        int limit = resolveLimit(method, path);
        String category = resolveCategory(method, path);
        String key = buildRateLimitKey(request, category);

        evictStaleEntriesIfNeeded();

        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(limit));

        if (!bucket.tryConsume()) {
            logger.warn("Rate limit exceeded for category [{}] with key [{}] on path [{}]", category, key, path);
            long retryAfterSeconds = Math.max(1, bucket.getSecondsUntilReset());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

            ErrorResponse errorResponse = new ErrorResponse(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                    "Rate limit exceeded. Please wait " + retryAfterSeconds + " seconds before trying again.",
                    path
            );
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private int resolveLimit(String method, String path) {
        if ("POST".equalsIgnoreCase(method)) {
            if (path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/register")) {
                return 10; // Auth: 10/min
            }
            if (path.contains("/voice/transcribe")) {
                return 15; // STT: 15/min
            }
            if (path.contains("/voice/synthesize")) {
                return 25; // TTS: 25/min
            }
            if (path.startsWith("/api/v1/coding/") && (path.endsWith("/run") || path.endsWith("/submit"))) {
                return 15; // Coding execution: 15/min
            }
            if (path.contains("/ai/generate-question")
                    || (path.contains("/resumes/") && path.endsWith("/analyze"))
                    || (path.contains("/job-descriptions/") && path.endsWith("/analyze"))
                    || (path.contains("/projects/") && path.endsWith("/analyze"))) {
                return 20; // AI generation: 20/min
            }
            if (path.contains("/ai/evaluate-answer")) {
                return 20; // AI evaluation: 20/min
            }
            if (path.contains("/ai/adaptive-follow-up")) {
                return 20; // AI follow-up: 20/min
            }
        }
        return 120; // General / default: 120/min
    }

    private String resolveCategory(String method, String path) {
        if ("POST".equalsIgnoreCase(method)) {
            if (path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/register")) return "AUTH";
            if (path.contains("/voice/transcribe")) return "VOICE_STT";
            if (path.contains("/voice/synthesize")) return "VOICE_TTS";
            if (path.startsWith("/api/v1/coding/")) return "CODING_EXEC";
            if (path.contains("/ai/generate-question") || path.contains("/analyze")) return "AI_GEN";
            if (path.contains("/ai/evaluate-answer")) return "AI_EVAL";
            if (path.contains("/ai/adaptive-follow-up")) return "AI_FOLLOWUP";
        }
        return "GENERAL";
    }

    private String buildRateLimitKey(HttpServletRequest request, String category) {
        if ("AUTH".equals(category)) {
            // Unauthenticated endpoints keyed strictly by client IP
            return "IP:" + getClientIp(request) + ":" + category;
        }

        // Authenticated endpoints keyed by authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return "USER:" + auth.getName() + ":" + category;
        }

        // Fallback to IP if unauthenticated
        return "IP:" + getClientIp(request) + ":" + category;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private void evictStaleEntriesIfNeeded() {
        if (buckets.size() >= MAX_ENTRIES) {
            long now = System.currentTimeMillis();
            buckets.entrySet().removeIf(entry -> (now - entry.getValue().lastRefillTime) > STALE_THRESHOLD_MS);

            // If still exceeding, drop oldest entries
            if (buckets.size() >= MAX_ENTRIES) {
                int toRemove = buckets.size() - (MAX_ENTRIES * 3 / 4);
                var iterator = buckets.keySet().iterator();
                while (iterator.hasNext() && toRemove > 0) {
                    iterator.next();
                    iterator.remove();
                    toRemove--;
                }
            }
        }
    }

    /**
     * Package-private test utility to reset limiter state deterministically between test cases.
     * NEVER exposed through any public HTTP endpoint.
     */
    void resetForTesting() {
        buckets.clear();
    }

    int getBucketCountForTesting() {
        return buckets.size();
    }

    /**
     * Thread-safe token bucket implementation with 60-second sliding replenishment.
     */
    static class TokenBucket {
        private final int capacity;
        private final AtomicInteger tokens;
        private volatile long lastRefillTime;

        TokenBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            refill();
            int current = tokens.get();
            if (current > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            if (elapsed >= 60000L) {
                tokens.set(capacity);
                lastRefillTime = now;
            }
        }

        long getSecondsUntilReset() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;
            long remaining = 60000L - elapsed;
            return remaining > 0 ? (remaining / 1000L) : 1L;
        }
    }
}
