package pers.lurker.keylock.lock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class KeyLock<T> implements Lock {
    private final static ConcurrentHashMap<Object, LockAndCounter> locksMap = new ConcurrentHashMap<>();

    private final T key;

    private KeyLock(T lockKey) {
        this.key = lockKey;
    }

    public static <T> KeyLock<T> getLock(T lockKey) {
        return new KeyLock<>(lockKey);
    }

    private static class LockAndCounter {
        private final Lock lock = new ReentrantLock();
        private final AtomicInteger counter = new AtomicInteger(0);
    }

    private LockAndCounter getLock() {
        return locksMap.compute(key, (key, lockAndCounterInner) -> {
            if (lockAndCounterInner == null) {
                lockAndCounterInner = new LockAndCounter();
            }
            lockAndCounterInner.counter.incrementAndGet();
            return lockAndCounterInner;
        });
    }

    private void cleanupLock(LockAndCounter lockAndCounterOuter) {
        if (lockAndCounterOuter.counter.decrementAndGet() == 0) {
            locksMap.compute(key, (key, lockAndCounterInner) -> {
                if (lockAndCounterInner == null || lockAndCounterInner.counter.get() == 0) {
                    return null;
                }
                return lockAndCounterInner;
            });
        }
    }

    @Override
    public void lock() {
        LockAndCounter lockAndCounter = getLock();
        lockAndCounter.lock.lock();
    }

    @Override
    public void unlock() {
        LockAndCounter lockAndCounter = locksMap.get(key);
        lockAndCounter.lock.unlock();
        cleanupLock(lockAndCounter);
    }


    @Override
    public void lockInterruptibly() throws InterruptedException {
        LockAndCounter lockAndCounter = getLock();

        try {
            lockAndCounter.lock.lockInterruptibly();
        } catch (InterruptedException e) {
            cleanupLock(lockAndCounter);
            throw e;
        }
    }

    @Override
    public boolean tryLock() {
        LockAndCounter lockAndCounter = getLock();
        boolean acquired = lockAndCounter.lock.tryLock();

        if (!acquired) {
            cleanupLock(lockAndCounter);
        }
        return acquired;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        LockAndCounter lockAndCounter = getLock();

        boolean acquired;
        try {
            acquired = lockAndCounter.lock.tryLock(time, unit);
        } catch (InterruptedException e) {
            cleanupLock(lockAndCounter);
            throw e;
        }

        if (!acquired) {
            cleanupLock(lockAndCounter);
        }

        return acquired;
    }

    @Override
    public Condition newCondition() {
        LockAndCounter lockAndCounter = locksMap.get(key);
        return lockAndCounter.lock.newCondition();
    }
}
