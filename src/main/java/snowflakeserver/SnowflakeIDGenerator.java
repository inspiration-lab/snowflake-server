package snowflakeserver;// SnowflakeIDGenerator.java
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

@Slf4j
public class SnowflakeIDGenerator {
    private static final int WORKER_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;
    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;
    private static final long MAX_CLOCK_DRIFT_MS = 1000L;

    private final long workerId;
    private final long epoch;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    private final StampedLock lock = new StampedLock();
    private final long initialNanoTime = System.nanoTime();
    private final long initialEpochMillis;

    public SnowflakeIDGenerator(long workerId, long epoch) {
        if (workerId < 0 || workerId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("Worker ID must be between 0 and " + MAX_WORKER_ID);
        }
        this.workerId = workerId;
        this.epoch = epoch;
        this.initialEpochMillis = System.currentTimeMillis();
    }

    /**
     * 不指定epoch时，默认为2021-01-01 00:00:00 UTC
     * @param workerId 服务器编号
     */
    public SnowflakeIDGenerator(long workerId) {
        this(workerId, 1609459200000L);
    }

    public long generateId() {
        long stamp = lock.writeLock();
        try {
            long currentTime = currentTime();

            if (currentTime < lastTimestamp) {
                handleClockDrift(currentTime);
                currentTime = currentTime();
            }

            if (currentTime == lastTimestamp) {
                sequence = (sequence + 1) & MAX_SEQUENCE;
                if (sequence == 0) {
                    currentTime = waitNextMillis(currentTime);
                }
            } else {
                sequence = 0L;
            }

            lastTimestamp = currentTime;

            return ((currentTime) << (WORKER_ID_BITS + SEQUENCE_BITS))
                   | (workerId << SEQUENCE_BITS)
                   | sequence;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private long currentTime() {
        long nanoDelta = System.nanoTime() - initialNanoTime;
        long deltaMillis = TimeUnit.NANOSECONDS.toMillis(nanoDelta);
        return (initialEpochMillis + deltaMillis) - epoch;
    }

    private void handleClockDrift(long currentTime) {
        long drift = lastTimestamp - currentTime;
        if (drift <= MAX_CLOCK_DRIFT_MS) {
            log.warn("Clock drifted backward by {}ms. Waiting for recovery...", drift);
            waitUntilClockRecovers();
        } else {
            String errorMsg = String.format("Critical clock drift detected: %dms (max allowed: %dms)",
                    drift, MAX_CLOCK_DRIFT_MS);
            log.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
    }

    private void waitUntilClockRecovers() {
        long currentTime = currentTime();
        while (currentTime <= lastTimestamp) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted during clock recovery");
            }
            currentTime = currentTime();
        }
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = currentTime();
        while (timestamp <= lastTimestamp) {
            Thread.onSpinWait();
            timestamp = currentTime();
        }
        return timestamp;
    }
}