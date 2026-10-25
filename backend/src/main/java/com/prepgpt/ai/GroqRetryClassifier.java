package com.prepgpt.ai;

import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

/**
 * Classifies Groq AI exceptions into retryable (transient) vs non-retryable (deterministic).
 *
 * Retryable:
 * - Network timeouts / Connection drops (ResourceAccessException, SocketTimeoutException, ConnectException)
 * - HTTP 429 (Rate Limit / Too Many Requests)
 * - HTTP 500, 502, 503, 504 (Server Errors / Bad Gateway / Unavailable)
 *
 * Non-Retryable:
 * - HTTP 400 (Bad Request / Schema validation)
 * - HTTP 401 (Unauthorized / Invalid API Key)
 * - HTTP 403 (Forbidden)
 * - HTTP 404 (Not Found)
 * - CallNotPermittedException (Circuit breaker is open)
 */
public class GroqRetryClassifier {

    public static boolean isRetryable(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        // If circuit breaker refused the call, do NOT retry
        if (throwable instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
            return false;
        }

        // Network / Socket level exceptions
        if (throwable instanceof ResourceAccessException ||
            throwable instanceof SocketTimeoutException ||
            throwable instanceof ConnectException ||
            throwable instanceof IOException) {
            return true;
        }

        // HTTP status level exceptions
        if (throwable instanceof HttpStatusCodeException httpEx) {
            int statusCode = httpEx.getStatusCode().value();
            if (statusCode == 429) {
                return true;
            }
            if (statusCode >= 500 && statusCode <= 599) {
                return true;
            }
            return false;
        }

        // Check cause chain
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            return isRetryable(cause);
        }

        return false;
    }
}
