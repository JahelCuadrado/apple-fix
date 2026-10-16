package com.einsteinmods.applefix.distanthorizons;

import com.einsteinmods.applefix.core.StoppableSleep;
import com.einsteinmods.applefix.core.TerminationWait;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Removes the freeze when leaving a world with Distant Horizons (seconds of an unresponsive window,
 * on every system).
 *
 * <p>On disconnect DH shuts its thread pools down from the render thread ({@code
 * ClientApi#onClientOnlyDisconnected} → {@code SharedApi#setDhWorld(null)} → {@code
 * ThreadPoolUtil#shutdownThreadPools} → {@code PriorityTaskPicker#shutdownNow}) and waits up to 5 s
 * for each pool in turn ({@code PriorityTaskPicker$Executor#awaitTermination}). Two things keep the
 * pools alive until those timeouts:
 *
 * <ul>
 *   <li>"Render Loader" workers blocked in {@code LodRenderSection#uploadRenderDataToGpuAsync} on a
 *       {@code CompletableFuture#join} for buffer tasks queued on {@code RenderThreadTaskHandler},
 *       which only the render thread runs: each side waits for the other. The render thread now
 *       runs that queue while it waits, as DH's own idle timer does between frames.
 *   <li>Idle sleeps on the "IO" workers: the throttle in {@code
 *       RateLimitedThreadPoolExecutor#afterExecute} (sleeps in proportion to the task's run time)
 *       and the 2 s pause after a beacon refresh ({@code
 *       LodQuadTree#tryRefreshRenderingBeaconsAsync}). They now end once their pool is shut down;
 *       they only pace work, they do not save anything.
 * </ul>
 *
 * <p>DH's timeouts are kept, so its saving gets at least the same time as before.
 */
public final class DistantHorizonsExitWait {
  private DistantHorizonsExitWait() {}

  /** Whether this wait must run DH's render-thread tasks: only on DH's render thread. */
  public static boolean mustRunRenderTasks() {
    return DistantHorizonsInternals.isOnRenderThread();
  }

  /** {@code ExecutorService#awaitTermination} that keeps running DH's render-thread tasks. */
  public static boolean awaitTermination(ExecutorService pool, long timeout, TimeUnit unit)
      throws InterruptedException {
    return TerminationWait.await(
        unit.toNanos(timeout), pool::isTerminated, DistantHorizonsInternals::runPendingRenderTasks);
  }

  /** DH's throttle sleep after a pool task; ends once {@code pool} is shut down. */
  public static void throttleSleep(ThreadPoolExecutor pool, long millis)
      throws InterruptedException {
    StoppableSleep.sleep(millis, pool::isShutdown);
  }

  /** DH's pause after a beacon refresh; ends once DH's file pool is shut down. */
  public static void beaconRefreshPause(long millis) throws InterruptedException {
    StoppableSleep.sleep(millis, DistantHorizonsInternals::isFilePoolShutDown);
  }
}
