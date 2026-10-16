package com.einsteinmods.applefix.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TerminationWaitTest {
  private static final long LONG_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);
  private static final long SHORT_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(50);

  /** The deadlock DH hits on exit: a pool thread joins a task only the waiting thread runs. */
  @Test
  void poolWaitingForTheWaitersQueueTerminatesQuickly() throws InterruptedException {
    ConcurrentLinkedQueue<Runnable> waiterQueue = new ConcurrentLinkedQueue<>();
    ExecutorService pool = Executors.newSingleThreadExecutor();
    pool.execute(
        () -> {
          CompletableFuture<Void> upload = new CompletableFuture<>();
          waiterQueue.add(() -> upload.complete(null));
          upload.join();
        });
    pool.shutdown();
    long start = System.nanoTime();

    boolean terminated =
        TerminationWait.await(LONG_TIMEOUT_NANOS, pool::isTerminated, () -> runAll(waiterQueue));

    assertTrue(terminated);
    assertTrue(System.nanoTime() - start < LONG_TIMEOUT_NANOS / 5);
  }

  @Test
  void keepsTheWholeTimeoutWhenThePoolNeverTerminates() throws InterruptedException {
    long start = System.nanoTime();

    boolean terminated = TerminationWait.await(SHORT_TIMEOUT_NANOS, () -> false, () -> {});

    assertFalse(terminated);
    assertTrue(System.nanoTime() - start >= SHORT_TIMEOUT_NANOS);
  }

  @Test
  void runsPendingWorkBeforeReportingTermination() throws InterruptedException {
    AtomicInteger runs = new AtomicInteger();

    boolean terminated =
        TerminationWait.await(LONG_TIMEOUT_NANOS, () -> true, runs::incrementAndGet);

    assertTrue(terminated);
    assertEquals(1, runs.get());
  }

  @Test
  void interruptStopsTheWaitLikeAwaitTermination() {
    Thread.currentThread().interrupt();

    assertThrows(
        InterruptedException.class,
        () -> TerminationWait.await(LONG_TIMEOUT_NANOS, () -> false, () -> {}));
    assertFalse(Thread.interrupted());
  }

  private static void runAll(ConcurrentLinkedQueue<Runnable> queue) {
    for (Runnable task = queue.poll(); task != null; task = queue.poll()) {
      task.run();
    }
  }
}
