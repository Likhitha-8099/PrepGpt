package com.prepgpt.config;

import com.prepgpt.ai.GroqRetryClassifier;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for external Groq AI interactions.
 * Configures Circuit Breaker and Bounded Exponential Backoff Retry.
 */
@Configuration
public class GroqResilienceConfig {

    private static final Logger logger = LoggerFactory.getLogger(GroqResilienceConfig.class);

    public static final String GROQ_CIRCUIT_BREAKER = "groq";
    public static final String GROQ_RETRY = "groq";

    @Bean
    public CircuitBreaker groqCircuitBreaker(
            @Value("${resilience4j.circuitbreaker.instances.groq.sliding-window-size:10}") int slidingWindowSize,
            @Value("${resilience4j.circuitbreaker.instances.groq.minimum-number-of-calls:5}") int minCalls,
            @Value("${resilience4j.circuitbreaker.instances.groq.failure-rate-threshold:50}") float failureRateThreshold,
            @Value("${resilience4j.circuitbreaker.instances.groq.wait-duration-in-open-state:30s}") Duration waitDurationInOpenState,
            @Value("${resilience4j.circuitbreaker.instances.groq.permitted-number-of-calls-in-half-open-state:2}") int permittedHalfOpenCalls) {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minCalls)
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(waitDurationInOpenState)
                .permittedNumberOfCallsInHalfOpenState(permittedHalfOpenCalls)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordException(GroqRetryClassifier::isRetryable)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker circuitBreaker = registry.circuitBreaker(GROQ_CIRCUIT_BREAKER);

        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> logger.warn("[Groq CircuitBreaker] State Transition: {} -> {}",
                        event.getStateTransition().getFromState(), event.getStateTransition().getToState()))
                .onError(event -> logger.warn("[Groq CircuitBreaker] Recorded failure: duration={}ms, error={}",
                        event.getElapsedDuration().toMillis(), event.getThrowable().getClass().getSimpleName()))
                .onCallNotPermitted(event -> logger.warn("[Groq CircuitBreaker] Call rejected: Circuit is OPEN"));

        return circuitBreaker;
    }

    @Bean
    public Retry groqRetry(
            @Value("${resilience4j.retry.instances.groq.max-attempts:3}") int maxAttempts,
            @Value("${resilience4j.retry.instances.groq.wait-duration:1000ms}") Duration initialWaitDuration,
            @Value("${resilience4j.retry.instances.groq.exponential-backoff-multiplier:2.0}") double backoffMultiplier) {

        IntervalFunction intervalFn = IntervalFunction.ofExponentialBackoff(initialWaitDuration, backoffMultiplier);

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .intervalFunction(intervalFn)
                .retryOnException(GroqRetryClassifier::isRetryable)
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        Retry retry = registry.retry(GROQ_RETRY);

        retry.getEventPublisher()
                .onRetry(event -> logger.info("[Groq Retry] Attempt #{} failed: error={}, waiting {}ms for next attempt",
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable().getClass().getSimpleName(),
                        event.getWaitInterval().toMillis()))
                .onError(event -> logger.warn("[Groq Retry] All {} attempts exhausted: error={}",
                        event.getNumberOfRetryAttempts(),
                        event.getLastThrowable().getClass().getSimpleName()));

        return retry;
    }
}
