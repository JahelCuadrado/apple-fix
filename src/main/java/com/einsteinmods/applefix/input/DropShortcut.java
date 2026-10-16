package com.einsteinmods.applefix.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.MacosUtil;
import net.minecraft.client.Minecraft;

/**
 * Lets Command, like Control, make the drop key throw the whole stack on macOS.
 *
 * <p>Vanilla 26.3 checks only Control ({@code Minecraft#hasControlDown}, {@code
 * InputWithModifiers#hasControlDown}); older versions checked Command on macOS, so both habits
 * exist. Command + Q also quits the game while vanilla's "Quit Shortcuts" option is on; then
 * Command is left alone, so quitting never throws a stack. Runs once per drop key press.
 */
public final class DropShortcut {
  private static final boolean IS_MAC = MacosUtil.IS_MACOS;

  private DropShortcut() {}

  /** Whether Command must count as Control for the drop key right now. */
  public static boolean commandDropsStack() {
    return IS_MAC
        && !Minecraft.getInstance().options.quitShortcuts().get()
        && (InputConstants.isKeyDown(InputConstants.KEY_LGUI)
            || InputConstants.isKeyDown(InputConstants.KEY_RGUI));
  }
}
