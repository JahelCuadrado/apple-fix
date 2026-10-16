package com.einsteinmods.applefix.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ThreadActivityTest {
  private static final StackTraceElement[] CACHED_POOL_IDLE = {
    frame("jdk.internal.misc.Unsafe", "park"),
    frame("java.util.concurrent.locks.LockSupport", "parkNanos"),
    frame("java.util.concurrent.LinkedTransferQueue$DualNode", "await"),
    frame("java.util.concurrent.SynchronousQueue$Transferer", "xferLifo"),
    frame("java.util.concurrent.SynchronousQueue", "xfer"),
    frame("java.util.concurrent.SynchronousQueue", "poll"),
    frame("java.util.concurrent.ThreadPoolExecutor", "getTask"),
    frame("java.util.concurrent.ThreadPoolExecutor", "runWorker"),
    frame("java.util.concurrent.ThreadPoolExecutor$Worker", "run"),
    frame("java.lang.Thread", "run"),
  };

  private static final StackTraceElement[] FIXED_POOL_IDLE = {
    frame("jdk.internal.misc.Unsafe", "park"),
    frame("java.util.concurrent.locks.LockSupport", "park"),
    frame("java.util.concurrent.LinkedBlockingQueue", "take"),
    frame("java.util.concurrent.ThreadPoolExecutor", "getTask"),
    frame("java.util.concurrent.ThreadPoolExecutor", "runWorker"),
    frame("java.lang.Thread", "run"),
  };

  private static final StackTraceElement[] TIMER_IDLE = {
    frame("java.lang.Object", "wait0"),
    frame("java.lang.Object", "wait"),
    frame("java.util.TimerThread", "mainLoop"),
    frame("java.util.TimerThread", "run"),
  };

  private static final StackTraceElement[] POOL_TASK_WRITING_FILE = {
    frame("sun.nio.ch.FileDispatcherImpl", "write0"),
    frame("sun.nio.ch.FileChannelImpl", "write"),
    frame("com.example.mod.SaveTask", "run"),
    frame("java.util.concurrent.ThreadPoolExecutor", "runWorker"),
    frame("java.lang.Thread", "run"),
  };

  private static final StackTraceElement[] POOL_TASK_WAITING_FOR_RESULT = {
    frame("jdk.internal.misc.Unsafe", "park"),
    frame("java.util.concurrent.locks.LockSupport", "park"),
    frame("java.util.concurrent.CompletableFuture", "get"),
    frame("com.example.mod.Uploader", "run"),
    frame("java.util.concurrent.ThreadPoolExecutor", "runWorker"),
    frame("java.lang.Thread", "run"),
  };

  private static final StackTraceElement[] TIMER_TASK_WAITING = {
    frame("java.lang.Object", "wait0"),
    frame("java.lang.Object", "wait"),
    frame("com.example.mod.FlushTask", "run"),
    frame("java.util.TimerThread", "mainLoop"),
    frame("java.util.TimerThread", "run"),
  };

  private static final StackTraceElement[] CUSTOM_QUEUE_IN_GET_TASK = {
    frame("jdk.internal.misc.Unsafe", "park"),
    frame("java.util.concurrent.locks.LockSupport", "park"),
    frame("com.example.mod.FlushingQueue", "take"),
    frame("java.util.concurrent.ThreadPoolExecutor", "getTask"),
    frame("java.lang.Thread", "run"),
  };

  private static final StackTraceElement[] SLEEPING_LOOP = {
    frame("java.lang.Thread", "sleepNanos0"),
    frame("java.lang.Thread", "sleep"),
    frame("com.example.mod.RetryLoop", "run"),
    frame("java.lang.Thread", "run"),
  };

  @Test
  void idlePoolThreadsAreIdle() {
    assertEquals(
        ThreadActivity.IDLE, ThreadActivity.of(Thread.State.TIMED_WAITING, CACHED_POOL_IDLE));
    assertEquals(ThreadActivity.IDLE, ThreadActivity.of(Thread.State.WAITING, FIXED_POOL_IDLE));
  }

  @Test
  void idleTimerThreadIsIdle() {
    assertEquals(ThreadActivity.IDLE, ThreadActivity.of(Thread.State.WAITING, TIMER_IDLE));
  }

  @Test
  void runningThreadIsWorkingWhateverItsStack() {
    assertEquals(
        ThreadActivity.WORKING, ThreadActivity.of(Thread.State.RUNNABLE, CACHED_POOL_IDLE));
    assertEquals(
        ThreadActivity.WORKING, ThreadActivity.of(Thread.State.RUNNABLE, POOL_TASK_WRITING_FILE));
  }

  @Test
  void blockedThreadIsWorking() {
    assertEquals(ThreadActivity.WORKING, ThreadActivity.of(Thread.State.BLOCKED, FIXED_POOL_IDLE));
  }

  @Test
  void poolTaskWaitingInsideModCodeIsWorking() {
    assertEquals(
        ThreadActivity.WORKING,
        ThreadActivity.of(Thread.State.WAITING, POOL_TASK_WAITING_FOR_RESULT));
    assertEquals(
        ThreadActivity.WORKING, ThreadActivity.of(Thread.State.WAITING, TIMER_TASK_WAITING));
  }

  @Test
  void modQueueAboveTheWaitPointIsWorking() {
    assertEquals(
        ThreadActivity.WORKING, ThreadActivity.of(Thread.State.WAITING, CUSTOM_QUEUE_IN_GET_TASK));
  }

  @Test
  void sleepingThreadIsWorking() {
    assertEquals(
        ThreadActivity.WORKING, ThreadActivity.of(Thread.State.TIMED_WAITING, SLEEPING_LOOP));
  }

  @Test
  void waitingThreadWithoutStackIsWorking() {
    assertEquals(
        ThreadActivity.WORKING, ThreadActivity.of(Thread.State.WAITING, new StackTraceElement[0]));
  }

  @Test
  void finishedOrUnstartedThreadsAreIdle() {
    assertEquals(
        ThreadActivity.IDLE, ThreadActivity.of(Thread.State.TERMINATED, new StackTraceElement[0]));
    assertEquals(
        ThreadActivity.IDLE, ThreadActivity.of(Thread.State.NEW, new StackTraceElement[0]));
  }

  /** Checks the frame names against real JDK pool threads, so a JDK update cannot drift them. */
  @Test
  void realIdlePoolThreadsAreIdle() throws InterruptedException {
    ExecutorService cached = Executors.newCachedThreadPool();
    ExecutorService fixed = Executors.newFixedThreadPool(1);
    ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor();
    try {
      assertEquals(ThreadActivity.IDLE, settledActivity(cached));
      assertEquals(ThreadActivity.IDLE, settledActivity(fixed));
      assertEquals(ThreadActivity.IDLE, settledActivity(scheduled));
    } finally {
      cached.shutdownNow();
      fixed.shutdownNow();
      scheduled.shutdownNow();
    }
  }

  @Test
  void realPoolThreadRunningATaskIsWorking() throws InterruptedException {
    ExecutorService pool = Executors.newSingleThreadExecutor();
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    Thread[] worker = new Thread[1];
    try {
      pool.execute(
          () -> {
            worker[0] = Thread.currentThread();
            started.countDown();
            awaitUninterruptibly(release);
          });
      started.await();
      awaitParked(worker[0]);
      assertEquals(
          ThreadActivity.WORKING,
          ThreadActivity.of(worker[0].getState(), worker[0].getStackTrace()));
    } finally {
      release.countDown();
      pool.shutdownNow();
    }
  }

  private static ThreadActivity settledActivity(ExecutorService pool) throws InterruptedException {
    Thread[] worker = new Thread[1];
    CountDownLatch ran = new CountDownLatch(1);
    pool.execute(
        () -> {
          worker[0] = Thread.currentThread();
          ran.countDown();
        });
    ran.await();
    awaitParked(worker[0]);
    return ThreadActivity.of(worker[0].getState(), worker[0].getStackTrace());
  }

  private static void awaitParked(Thread thread) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (thread.getState() == Thread.State.RUNNABLE && System.nanoTime() < deadline) {
      Thread.sleep(5);
    }
  }

  private static void awaitUninterruptibly(CountDownLatch latch) {
    boolean interrupted = false;
    while (true) {
      try {
        latch.await();
        break;
      } catch (InterruptedException e) {
        interrupted = true;
      }
    }
    if (interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  private static StackTraceElement frame(String className, String methodName) {
    return new StackTraceElement(className, methodName, null, -1);
  }
}
