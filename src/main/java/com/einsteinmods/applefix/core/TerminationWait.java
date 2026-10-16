package com.einsteinmods.applefix.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

/**
 * Waits for a thread pool to terminate while running work its threads wait for on the waiting
 * thread, so the pool and the waiter cannot block each other until the timeout expires.
 */
public final class TerminationWait {
  /** Pause between checks when nothing changed: termination is noticed within this delay. */
  static final long POLL_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

  private TerminationWait() {}

  /**
   * Same contract as {@code ExecutorService#awaitTermination}: the full {@code timeoutNanos} is
   * kept, only the waiting changes.
   *
   * @param timeoutNanos longest wait
   * @param terminated whether the pool has terminated
   * @param pendingWork runs the work queued for the waiting thread; returns without blocking
   * @return {@code true} if the pool terminated, {@code false} if the timeout expired first
   * @throws InterruptedException if the waiting thread is interrupted
   */
  public static boolean await(long timeoutNanos, BooleanSupplier terminated, Runnable pendingWork)
      throws InterruptedException {
    long deadline = System.nanoTime() + timeoutNanos;
    while (true) {
      pendingWork.run();
      if (terminated.getAsBoolean()) {
        return true;
      }
      long remaining = deadline - System.nanoTime();
      if (remaining <= 0) {
        return false;
      }
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      LockSupport.parkNanos(Math.min(POLL_NANOS, remaining));
    }
  }
}
