package com.einsteinmods.applefix.dialog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MainThreadDispatcherTest {
  private static final String MAIN_THREAD_NAME = "test main thread";

  private ExecutorService mainThread;
  private MainThreadDispatcher dispatcher;

  @BeforeEach
  void startMainThread() {
    mainThread = Executors.newSingleThreadExecutor(task -> new Thread(task, MAIN_THREAD_NAME));
    dispatcher =
        new MainThreadDispatcher(
            () -> Thread.currentThread().getName().equals(MAIN_THREAD_NAME), mainThread);
  }

  @AfterEach
  void stopMainThread() throws InterruptedException {
    mainThread.shutdownNow();
    assertTrue(mainThread.awaitTermination(5, TimeUnit.SECONDS));
  }

  @Test
  void callOnMainThreadRunsDirectlyWithoutScheduling() {
    AtomicReference<Thread> ranOn = new AtomicReference<>();
    MainThreadDispatcher onMainThread =
        new MainThreadDispatcher(
            () -> true,
            task -> {
              throw new AssertionError("must not schedule");
            });

    onMainThread.runAndWait(() -> ranOn.set(Thread.currentThread()));

    assertSame(Thread.currentThread(), ranOn.get());
  }

  @Test
  void callOffMainThreadRunsOnMainThreadAndReturnsAfterIt() {
    AtomicReference<String> ranOn = new AtomicReference<>();
    AtomicBoolean finished = new AtomicBoolean();

    dispatcher.runAndWait(
        () -> {
          sleepBriefly();
          ranOn.set(Thread.currentThread().getName());
          finished.set(true);
        });

    assertTrue(finished.get(), "returned before the call finished");
    assertSame(MAIN_THREAD_NAME, ranOn.get());
  }

  @Test
  void exceptionOfTheCallIsRethrownToTheCaller() {
    IllegalArgumentException failure = new IllegalArgumentException("dialog failed");

    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                dispatcher.runAndWait(
                    () -> {
                      throw failure;
                    }));

    assertSame(failure, thrown);
  }

  @Test
  void interruptDoesNotEndTheWaitAndIsKeptForTheCaller() throws InterruptedException {
    CountDownLatch callStarted = new CountDownLatch(1);
    CountDownLatch releaseCall = new CountDownLatch(1);
    AtomicBoolean callFinishedBeforeReturn = new AtomicBoolean();
    AtomicBoolean interruptKept = new AtomicBoolean();
    AtomicBoolean callFinished = new AtomicBoolean();
    Thread caller =
        new Thread(
            () -> {
              dispatcher.runAndWait(
                  () -> {
                    callStarted.countDown();
                    awaitQuietly(releaseCall);
                    callFinished.set(true);
                  });
              callFinishedBeforeReturn.set(callFinished.get());
              interruptKept.set(Thread.currentThread().isInterrupted());
            });

    caller.start();
    assertTrue(callStarted.await(5, TimeUnit.SECONDS));
    caller.interrupt();
    caller.join(100);
    assertTrue(caller.isAlive(), "caller returned while the call was still running");
    releaseCall.countDown();
    caller.join(5_000);

    assertFalse(caller.isAlive());
    assertTrue(callFinishedBeforeReturn.get());
    assertTrue(interruptKept.get());
  }

  private static void sleepBriefly() {
    try {
      Thread.sleep(50);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }

  private static void awaitQuietly(CountDownLatch latch) {
    try {
      assertTrue(latch.await(5, TimeUnit.SECONDS));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
    }
  }
}
