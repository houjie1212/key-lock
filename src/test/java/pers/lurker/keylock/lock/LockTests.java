package pers.lurker.keylock.lock;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Slf4j
public class LockTests {

    @Test
    void testLock() {
        int executeThreads = 8;
        ExecutorService exectorService = Executors.newFixedThreadPool(executeThreads);

        CompletableFuture.allOf(IntStream.range(0, executeThreads)
                .mapToObj(i -> CompletableFuture.runAsync(() -> {
                            KeyLock<String> lock = KeyLock.getLock("test" + i % 3);
                            lock.lock();
                            log.info("{} start", Thread.currentThread().getName());
                            try {
                                TimeUnit.SECONDS.sleep(5);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } finally {
                                lock.unlock();
                            }
                            log.info("{} end", Thread.currentThread().getName());
                        }, exectorService)
                ).toArray(CompletableFuture[]::new)).join();
    }
}
