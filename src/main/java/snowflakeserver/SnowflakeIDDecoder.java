package snowflakeserver;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class SnowflakeIDDecoder {
    public static long parseTimestamp(long snowflakeId, long epoch) {
        return (snowflakeId >> 22) + epoch;
    }

    public static LocalDateTime parseDateTime(long snowflakeId, long epoch, ZoneOffset zoneOffset) {
        long timestamp = parseTimestamp(snowflakeId, epoch);
        Instant instant = Instant.ofEpochMilli(timestamp);
        return LocalDateTime.ofInstant(instant, zoneOffset);
    }

    public static LocalDateTime parseDateTime(long snowflakeId, long epoch) {
        // 偏移8小时，转化为国内时间
        return parseDateTime(snowflakeId, epoch, ZoneOffset.ofHours(8));
    }
}
