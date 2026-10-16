package com.einsteinmods.applefix.dialog;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.function.BooleanSupplier;

/**
 * Runs a call on the main thread and blocks the calling thread until it has finished.
 *
 * <p>The caller must stay blocked for the whole call: native arguments often point to memory the
 * caller frees on return (for example its LWJGL {@code MemoryStack} frame), so returning early
 * would hand freed memory to the native side. That is also why an interrupt does not end the wait.
 *
 * <p>Deadlocks if the main thread itself waits for the calling thread; callers that did that before
 * crashed on macOS instead (AppKit aborts when a window is created off the main thread).
 */
public final class MainThreadDispatcher {
  private final BooleanSupplier isMainThread;
  private final Executor mainThreadExecutor;

  /**
   * @param isMainThread whether the current thread is the one {@code mainThreadExecutor} runs on
   * @param mainThreadExecutor runs tasks on the main thread
   */
  public MainThreadDispatcher(BooleanSupplier isMainThread, Executor mainThreadExecutor) {
    this.isMainThread = Objects.requireNonNull(isMainThread);
    this.mainThreadExecutor = Objects.requireNonNull(mainThreadExecutor);
  }

  /**
   * Runs {@code call} directly on the main thread, otherwise on the main thread while this thread
   * waits. Exceptions thrown by {@code call} are rethrown here, never on the main thread.
   */
  public void runAndWait(Runnable call) {
    if (isMainThread.getAsBoolean()) {
      call.run();
      return;
    }
    FutureTask<Void> task = new FutureTask<>(call, null);
    mainThreadExecutor.execute(task);
    awaitUninterruptibly(task);
  }

  private static void awaitUninterruptibly(FutureTask<Void> task) {
    boolean interrupted = false;
    try {
      while (true) {
        try {
          task.get();
          return;
        } catch (InterruptedException exception) {
          interrupted = true;
        } catch (ExecutionException exception) {
          throw rethrow(exception.getCause());
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static RuntimeException rethrow(Throwable cause) {
    return switch (cause) {
      case RuntimeException runtimeException -> runtimeException;
      case Error error -> throw error;
      default -> new IllegalStateException(cause);
    };
  }
}
