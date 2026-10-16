package com.einsteinmods.applefix.dialog;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

/**
 * Moves SDL file dialogs opened off the main thread onto it on macOS.
 *
 * <p>AppKit only allows windows on the main thread: SDL's Cocoa backend creates an {@code
 * NSSavePanel}/{@code NSOpenPanel} on the calling thread, which aborts the game when that is not
 * the main thread. The client game thread is the main thread: {@code
 * net.minecraft.client.main.Main#main} renames the launcher thread (thread 0 under {@code
 * -XstartOnFirstThread}) to "Render thread" and runs {@code Minecraft#run} on it, which is also the
 * thread {@code Minecraft#isSameThread} checks and {@code Minecraft#execute} schedules on.
 */
public final class MacFileDialogs {
  private static final boolean IS_MAC = Util.getPlatform() == Util.OS.OSX;
  private static final MainThreadDispatcher DISPATCHER =
      new MainThreadDispatcher(MacFileDialogs::isOnGameThread, MacFileDialogs::executeOnGameThread);

  private MacFileDialogs() {}

  /**
   * Whether a dialog call on the current thread must be moved to the main thread. One static field
   * read on other systems.
   */
  public static boolean mustMoveToMainThread() {
    return IS_MAC && !isOnGameThread();
  }

  /** Runs {@code dialogCall} on the main thread and waits until it returns. */
  public static void runOnMainThread(Runnable dialogCall) {
    DISPATCHER.runAndWait(dialogCall);
  }

  /** Before the client exists nothing can be scheduled, so the call is left where it is. */
  private static boolean isOnGameThread() {
    Minecraft minecraft = Minecraft.getInstance();
    return minecraft == null || minecraft.isSameThread();
  }

  private static void executeOnGameThread(Runnable task) {
    Minecraft.getInstance().execute(task);
  }
}
