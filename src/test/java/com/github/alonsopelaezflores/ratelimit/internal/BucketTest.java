package com.github.alonsopelaezflores.ratelimit.internal;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class BucketTest {


    @Test
    void allowsConsumingUpToCapacity() {
        Bucket bucket = new Bucket(5, 5, 1000);
        for (int i = 0; i < 5; i++) {
            assertTrue(bucket.tryConsume());
        }
        assertFalse(bucket.tryConsume());
    }

    @Test
    void refillsOverTime() throws InterruptedException {
        Bucket bucket = new Bucket(5, 5, 1000);
        for (int i = 0; i < 5; i++) bucket.tryConsume();
        assertFalse(bucket.tryConsume());

        Thread.sleep(1100);
        assertTrue(bucket.tryConsume());
    }

    @Test
    void doesNotExceedCapacityUnderConcurrency() throws InterruptedException {
        Bucket bucket = new Bucket(10, 10, 1000);
        int numThreads = 50;
        AtomicInteger successful = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                if (bucket.tryConsume()) successful.incrementAndGet();
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdown();

        assertEquals(10, successful.get());
    }
}
