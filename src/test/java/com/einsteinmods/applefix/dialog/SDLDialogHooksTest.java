package com.einsteinmods.applefix.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.lwjgl.sdl.SDLDialog;

/**
 * Checks SDLDialogMixin (outside its package: mixin packages cannot load other classes). Fails if
 * LWJGL's {@code SDLDialog} stops being transformable or a hook stops applying. Runs on Fabric
 * Loader's Knot class loader (fabric-loader-junit, client environment), as in game.
 */
class SDLDialogHooksTest {
  private static final int DIALOG_HOOKS = 4;
  private static final String HOOK_PREFIX = "handler$";
  private static final String MOD_ID = "applefix$";

  @Test
  void everyFileDialogHookIsMergedIntoSdlDialog() {
    Method[] methods = SDLDialog.class.getDeclaredMethods();

    long hooks =
        Arrays.stream(methods)
            .filter(m -> m.getName().startsWith(HOOK_PREFIX) && m.getName().contains(MOD_ID))
            .count();

    assertEquals(DIALOG_HOOKS, hooks, () -> "merged methods: " + Arrays.toString(methods));
  }
}
