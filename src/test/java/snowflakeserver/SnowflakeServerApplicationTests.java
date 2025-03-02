package snowflakeserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class SnowflakeServerApplicationTests {

    private static final int THREAD_COUNT = 1000;

    @Test
    void testBasicGeneration() throws InterruptedException {
        SnowflakeIDGenerator generator = new SnowflakeIDGenerator(1);
        System.out.println("Generating 10 IDs:");
        for (int i = 0; i < 10; i++) {
            long id = generator.generateId();
            LocalDateTime dt = SnowflakeIDDecoder.parseDateTime(id, 1609459200000L, ZoneOffset.ofHours(8));
            System.out.printf("ID: %d | Time: %s%n", id, dt);
        }
    }

    @Test
    void testConcurrentGeneration() throws InterruptedException {
        System.out.println("\nTesting multi-thread generation...");
        Set<Long> ids = Collections.newSetFromMap(new ConcurrentHashMap<>());
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        SnowflakeIDGenerator generator = new SnowflakeIDGenerator(2);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.execute(() -> {
                long id = generator.generateId();
                ids.add(id);
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println("Generated " + ids.size() + " unique IDs. No duplicates: " +
                           (ids.size() == THREAD_COUNT));
    }

    @Test
    void testErrorCases() {
        try {
            new SnowflakeIDGenerator(1025);
        } catch (IllegalArgumentException e) {
            System.out.println("\nInvalid worker ID test passed: " + e.getMessage());
        }
    }

}
