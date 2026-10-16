package com.einsteinmods.applefix.core;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** {@code Thread.sleep} that ends early, within one slice, once a stop condition holds. */
public final class StoppableSleep {
  /** Longest delay between a stop and the sleeper noticing it. */
  static final long SLICE_MILLIS = 50L;

  private static final long SLICE_NANOS = TimeUnit.MILLISECONDS.toNanos(SLICE_MILLIS);

  private StoppableSleep() {}

  /**
   * Same contract as {@code Thread.sleep(millis)} (including its exceptions), but returns as soon
   * as {@code stop} is true, checked before each slice.
   */
  public static void sleep(long millis, BooleanSupplier stop) throws InterruptedException {
    if (millis <= 0) {
      Thread.sleep(millis); // same exception (negative) and interrupt check (zero) as vanilla
      return;
    }
    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
    long remainingNanos = TimeUnit.MILLISECONDS.toNanos(millis);
    while (remainingNanos > 0) {
      if (stop.getAsBoolean()) {
        return;
      }
      // Nanosecond precision: truncating to millis would wake up to 1 ms early.
      Thread.sleep(Duration.ofNanos(Math.min(remainingNanos, SLICE_NANOS)));
      remainingNanos = deadline - System.nanoTime();
    }
  }
}
