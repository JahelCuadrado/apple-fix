package com.einsteinmods.applefix.distanthorizons;

import com.mojang.logging.LogUtils;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;

/**
 * Reaches the Distant Horizons internals the exit fix needs without a compile dependency (DH
 * 3.3.4): {@code core.render.RenderThreadTaskHandler}'s public {@code INSTANCE}, {@code
 * isCurrentThread()} and {@code runRenderThreadTasks()}, and {@code
 * core.util.threading.ThreadPoolUtil#getFileHandlerExecutor()}. They are looked up once, the first
 * time a hooked DH method asks. If any is gone (DH changed), one warning is logged and every query
 * answers as if AppleFix were absent, so DH keeps its own behaviour.
 */
final class DistantHorizonsInternals {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final String PACKAGE = "com.seibel.distanthorizons.core.";
  private static final String RENDER_TASKS_CLASS = PACKAGE + "render.RenderThreadTaskHandler";
  private static final String THREAD_POOLS_CLASS = PACKAGE + "util.threading.ThreadPoolUtil";

  private DistantHorizonsInternals() {}

  /** Whether the current thread is the one DH runs its render-thread tasks on. */
  static boolean isOnRenderThread() {
    Handles handles = Handles.INSTANCE;
    if (handles == null) {
      return false;
    }
    try {
      return (boolean) handles.isRenderThread().invokeExact();
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable t) {
      throw new IllegalStateException(t);
    }
  }

  /** Runs DH's queued render-thread tasks, as DH does between frames; returns when it pauses. */
  static void runPendingRenderTasks() {
    Handles handles = Handles.INSTANCE;
    if (handles == null) {
      return;
    }
    try {
      handles.runRenderTasks().invokeExact();
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable t) {
      throw new IllegalStateException(t);
    }
  }

  /** Whether DH's file (IO) thread pool exists and has been shut down. */
  static boolean isFilePoolShutDown() {
    Handles handles = Handles.INSTANCE;
    if (handles == null) {
      return false;
    }
    Object pool;
    try {
      pool = handles.filePool().invokeExact();
    } catch (RuntimeException | Error e) {
      throw e;
    } catch (Throwable t) {
      throw new IllegalStateException(t);
    }
    return pool instanceof ExecutorService executor && executor.isShutdown();
  }

  /** Bound handles; {@code INSTANCE} is null when DH's internals do not match. */
  private record Handles(
      MethodHandle isRenderThread, MethodHandle runRenderTasks, MethodHandle filePool) {
    static final Handles INSTANCE = find();

    private static Handles find() {
      try {
        ClassLoader loader = DistantHorizonsInternals.class.getClassLoader();
        Class<?> renderTasks = Class.forName(RENDER_TASKS_CLASS, true, loader);
        Class<?> threadPools = Class.forName(THREAD_POOLS_CLASS, true, loader);
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        Object renderTaskHandler =
            lookup.findStaticGetter(renderTasks, "INSTANCE", renderTasks).invoke();
        return new Handles(
            lookup
                .findVirtual(renderTasks, "isCurrentThread", MethodType.methodType(boolean.class))
                .bindTo(renderTaskHandler),
            lookup
                .findVirtual(renderTasks, "runRenderThreadTasks", MethodType.methodType(void.class))
                .bindTo(renderTaskHandler),
            lookup
                .unreflect(threadPools.getMethod("getFileHandlerExecutor"))
                .asType(MethodType.methodType(Object.class)));
      } catch (Throwable t) {
        LOGGER.warn(
            "AppleFix: Distant Horizons internals changed ({}); its world exit freeze fix is off.",
            t.toString());
        return null;
      }
    }
  }
}
