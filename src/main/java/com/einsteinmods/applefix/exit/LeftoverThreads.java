package com.einsteinmods.applefix.exit;

import com.einsteinmods.applefix.core.ThreadActivity;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

/**
 * Ends the process once the game has shut down when the only threads still keeping the JVM alive
 * are idle pool or timer threads some mod forgot to stop.
 *
 * <p>Without this, the JVM waits for those threads (a cached pool keeps its idle threads for 60 s)
 * and vanilla's post-main {@code ClientShutdownWatchdog} gives up after 15 s, writes a false
 * "Client shutdown from post-main" crash report and exits with -8.
 *
 * <p>Runs once, on the main thread, after {@code Main#main} has finished its own shutdown. Threads
 * doing work keep the process alive: they get {@link #WORKING_GRACE_MILLIS} to finish (checking
 * again every {@link #POLL_MILLIS}); if they are still busy after that, nothing is done and vanilla
 * behaves as it always did. Exits through {@link System#exit}, so shutdown hooks still run.
 */
public final class LeftoverThreads {
  private static final Logger LOGGER = LogUtils.getLogger();

  private static final long POLL_MILLIS = 200L;

  /** Well below the 15 s of vanilla's post-main watchdog. */
  private static final long WORKING_GRACE_MILLIS = 5_000L;

  /** Consecutive all-idle checks needed, so a task handed over between two checks is not lost. */
  private static final int IDLE_CHECKS_TO_EXIT = 2;

  private static final int NORMAL_EXIT_STATUS = 0;

  private LeftoverThreads() {}

  /** Called on the main thread once the game has finished shutting down. */
  public static void endIfOnlyIdleThreadsRemain() {
    Thread self = Thread.currentThread();
    long deadline = System.nanoTime() + WORKING_GRACE_MILLIS * 1_000_000L;
    int idleChecks = 0;
    while (true) {
      List<String> leftovers = new ArrayList<>();
      boolean working = false;
      for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
        Thread thread = entry.getKey();
        if (thread == self || thread.isDaemon() || !thread.isAlive()) {
          continue;
        }
        leftovers.add(thread.getName());
        if (ThreadActivity.of(thread.getState(), entry.getValue()) == ThreadActivity.WORKING) {
          working = true;
        }
      }
      if (leftovers.isEmpty()) {
        // Normal exit: the JVM ends by itself when this thread returns.
        return;
      }
      idleChecks = working ? 0 : idleChecks + 1;
      if (idleChecks >= IDLE_CHECKS_TO_EXIT) {
        LOGGER.info(
            "AppleFix: ending the game now; mods left these idle background threads running: {}",
            String.join(", ", leftovers));
        System.exit(NORMAL_EXIT_STATUS);
        return;
      }
      if (System.nanoTime() - deadline > 0) {
        // Something is still working: let vanilla wait for it, as it always did.
        return;
      }
      try {
        Thread.sleep(POLL_MILLIS);
      } catch (InterruptedException e) {
        self.interrupt();
        return;
      }
    }
  }
}
