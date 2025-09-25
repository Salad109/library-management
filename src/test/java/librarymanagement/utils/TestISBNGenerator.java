package librarymanagement.utils;

import java.util.concurrent.atomic.AtomicLong;

public final class TestISBNGenerator {
    private static final AtomicLong counter = new AtomicLong(1);
    private static final String BASE_PREFIX = "978000000";

    private TestISBNGenerator() {
    }

    public static String next() {
        long nextId = counter.getAndIncrement();
        return BASE_PREFIX + String.format("%04d", nextId);
    }
}