package net.magnesiumbackend.devtools;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RegistrySwapper<R> {
    private final AtomicReference<R> active;
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public RegistrySwapper(R initial) {
        this.active = new AtomicReference<>(initial);
    }

    public R acquire() {
        inFlight.incrementAndGet();
        return active.get();
    }

    public void release() {
        inFlight.decrementAndGet();
    }

    public void swap(R newRegistry, boolean drainFirst) throws InterruptedException {
        if (drainFirst) {
            while (inFlight.get() > 0) {
                Thread.sleep(1);
            }
        }
        active.set(newRegistry);
    }

    public R current() { return active.get(); }
}