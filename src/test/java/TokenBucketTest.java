import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author nedis
 * @since 1.0
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
final class TokenBucketTest {

    // Must be more than 50!!!
    private static final int SINGLE_THREAD_ITERATION_COUNT = 550;

    // Must be less than 50 / 2 for concurrency test!!!
    private static final int MULTI_THREAD_ITERATION_COUNT = 15;

    // Creates token bucket that supports 50 requests per 200 millis
    private final TokenBucket tokenBucket = new TokenBucket(50, 200, TimeUnit.MILLISECONDS);

    @Test
    void Should_process_50_requests_per_200_millis_using_single_thread() {
        int processedRequests = 0;
        for (int i = 0; i < SINGLE_THREAD_ITERATION_COUNT; i++) {
            if (tokenBucket.isRequestValid()) {
                processedRequests++;
            }
        }

        assertEquals(50, processedRequests);
    }

    @RepeatedTest(15)
    void Should_process_250_requests_per_1_second_using_single_thread() {
        final long maxTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1);
        int processedRequests = 0;
        do {
            for (int i = 0; i < SINGLE_THREAD_ITERATION_COUNT; i++) {
                if (tokenBucket.isRequestValid()) {
                    processedRequests++;
                }
            }
            sleepCurrentThread(50, TimeUnit.MILLISECONDS);
        } while (System.currentTimeMillis() <= maxTime);

        assertEquals(250, processedRequests);
    }

    @RepeatedTest(50)
    void Should_process_150_requests_per_900_millis_using_10_threads() {
        final int threadCount = 10;
        final AtomicInteger processedRequests = new AtomicInteger();
        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                for (int k = 0; k < 3; k++) {
                    sleepCurrentThread(300, TimeUnit.MILLISECONDS);
                    for (int j = 0; j < MULTI_THREAD_ITERATION_COUNT; j++) {
                        if (tokenBucket.isRequestValid()) {
                            processedRequests.incrementAndGet();
                        }
                    }
                }
            }));
        }
        waitForAllRunnableCompleted(futures);
        executorService.shutdown();

        assertEquals(150, processedRequests.get());
    }

    private void waitForAllRunnableCompleted(final List<Future<?>> futures) {
        for (final Future<?> future : futures) {
            try {
                future.get();
            } catch (final InterruptedException | ExecutionException exception) {
                exception.printStackTrace();
            }
        }
    }

    private void sleepCurrentThread(final long value,
                                    final TimeUnit timeUnit) {
        try {
            timeUnit.sleep(value);
        } catch (final InterruptedException exception) {
            exception.printStackTrace();
        }
    }
}