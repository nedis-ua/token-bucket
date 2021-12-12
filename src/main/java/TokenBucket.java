import java.util.concurrent.TimeUnit;

/**
 * TokenBucket object size in RAM:
 *  - 12 bytes for std Java object header
 *  - 4 * sizeOf(long) = 4 * 8 = 32 bytes
 *    Total: 44 bytes per TokenBucket object.
 * ----------------------------------------------------------------------------------------------------------
 * For real cases we can use: int (4 bytes) instead of long (8 bytes) for the following fields:
 *  - maxRequestsPerTimeInterval        -> max value: 2 147 483 647 -> 2 billions requests per time interval
 *  - availableRequestsPerTimeInterval  -> max value: 2 147 483 647 -> 2 billions requests per time interval
 *  - timeIntervalStepInMillis          -> max value: 2 147 483 647 millis -> 25 days
 *
 * Total will be: 12 + 8 + 4 + 4 + 4 = 32 bytes per TokenBucket object.
 *
 * @author nedis
 * @since 1.0
 */
public final class TokenBucket {

    private final long maxRequestsPerTimeInterval;

    private final long timeIntervalStepInMillis;

    private long availableRequestsPerTimeInterval;

    private long nextUpdateTimeInMillis;

    public TokenBucket(final long maxRequestsPerTimeInterval,
                       final int timeIntervalValue,
                       final TimeUnit timeIntervalUnit) {
        validateMethodParameters(maxRequestsPerTimeInterval, timeIntervalValue, timeIntervalUnit);
        this.maxRequestsPerTimeInterval = maxRequestsPerTimeInterval;
        this.timeIntervalStepInMillis = timeIntervalUnit.toMillis(timeIntervalValue);
    }

    public synchronized boolean isRequestValid() {
        updateBucket();
        if (this.availableRequestsPerTimeInterval > 0) {
            this.availableRequestsPerTimeInterval--;
            return true;
        } else {
            return false;
        }
    }

    private void updateBucket() {
        final long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis > this.nextUpdateTimeInMillis) {
            this.nextUpdateTimeInMillis = currentTimeMillis + this.timeIntervalStepInMillis;
            this.availableRequestsPerTimeInterval = this.maxRequestsPerTimeInterval;
        }
    }

    private void validateMethodParameters(final long maxRequestsPerTimeInterval,
                                          final int timeIntervalValue,
                                          final TimeUnit timeIntervalUnit) {
        if (maxRequestsPerTimeInterval < 0) {
            throw new IllegalArgumentException("maxRequestsPerTimeInterval must be > 0!");
        }
        if (timeIntervalValue < 0) {
            throw new IllegalArgumentException("timeIntervalValue must be > 0!");
        }
        if (timeIntervalUnit == TimeUnit.NANOSECONDS || timeIntervalUnit == TimeUnit.MICROSECONDS) {
            throw new IllegalArgumentException("Sorry, but nanoseconds or microseconds are not supported now! Please use other time units!");
        }
    }
}
