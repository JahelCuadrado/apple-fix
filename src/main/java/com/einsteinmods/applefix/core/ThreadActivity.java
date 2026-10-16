package com.einsteinmods.applefix.core;

/**
 * Whether a thread left running after the game shut down is doing work or only waiting for work
 * that will never come. Decided from the thread state and its stack, top frame first.
 *
 * <p>A thread is idle only when it waits inside the JDK's own "wait for the next task" code of a
 * thread pool or timer, with nothing but JDK frames above that point. Everything else (running,
 * blocked on a lock, sleeping, waiting in mod code, empty or unknown stack) counts as working, so a
 * thread that might be writing data is never taken for idle.
 */
public enum ThreadActivity {
  IDLE,
  WORKING;

  /** Frames where a pool or timer thread waits for its next task, as (class, method) pairs. */
  private static final String[][] IDLE_WAIT_POINTS = {
    // ThreadPoolExecutor and ScheduledThreadPoolExecutor workers (Executors.new*Pool): getTask
    // blocks in the work queue's take/poll (SynchronousQueue, LinkedBlockingQueue,
    // DelayedWorkQueue...) while no task is queued.
    {"java.util.concurrent.ThreadPoolExecutor", "getTask"},
    // ForkJoinPool workers with a custom non-daemon factory.
    {"java.util.concurrent.ForkJoinPool", "awaitWork"},
    // java.util.Timer threads (non-daemon by default) between tasks.
    {"java.util.TimerThread", "mainLoop"},
  };

  private static final String[] JDK_PACKAGES = {"java.", "javax.", "jdk.", "sun."};

  /**
   * Classifies a thread.
   *
   * @param state the thread state
   * @param stack the thread stack, top frame first (as {@link Thread#getStackTrace()})
   */
  public static ThreadActivity of(Thread.State state, StackTraceElement[] stack) {
    return switch (state) {
      // Not started or already finished: it does not keep the process alive.
      case NEW, TERMINATED -> IDLE;
      case WAITING, TIMED_WAITING -> waitsForNextTask(stack) ? IDLE : WORKING;
      case RUNNABLE, BLOCKED -> WORKING;
    };
  }

  private static boolean waitsForNextTask(StackTraceElement[] stack) {
    for (StackTraceElement frame : stack) {
      if (isIdleWaitPoint(frame)) {
        return true;
      }
      if (!isJdkFrame(frame)) {
        // Mod code above the wait point (a task, a custom queue): it may be doing work.
        return false;
      }
    }
    return false;
  }

  private static boolean isIdleWaitPoint(StackTraceElement frame) {
    for (String[] waitPoint : IDLE_WAIT_POINTS) {
      if (waitPoint[0].equals(frame.getClassName()) && waitPoint[1].equals(frame.getMethodName())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isJdkFrame(StackTraceElement frame) {
    String className = frame.getClassName();
    for (String jdkPackage : JDK_PACKAGES) {
      if (className.startsWith(jdkPackage)) {
        return true;
      }
    }
    return false;
  }
}
