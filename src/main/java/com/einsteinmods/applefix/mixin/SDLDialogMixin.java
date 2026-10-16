package com.einsteinmods.applefix.mixin;

import com.einsteinmods.applefix.dialog.MacFileDialogs;
import org.lwjgl.sdl.SDLDialog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Opens SDL file dialogs on the main thread on macOS; see MacFileDialogs.
 *
 * <p>Targets the unsafe {@code nSDL_Show*} methods: every public overload of {@code SDLDialog}
 * funnels into one of them right before the native call, after encoding its arguments (LWJGL 3.4.3
 * {@code SDLDialog}). The re-dispatched call runs on the main thread, where these hooks let it
 * through.
 */
@Mixin(SDLDialog.class)
abstract class SDLDialogMixin {
  @Inject(method = "nSDL_ShowOpenFileDialog", at = @At("HEAD"), cancellable = true)
  private static void applefix$openFileOnMainThread(
      long callback,
      long userdata,
      long window,
      long filters,
      int filterCount,
      long defaultLocation,
      boolean allowMany,
      CallbackInfo ci) {
    if (MacFileDialogs.mustMoveToMainThread()) {
      MacFileDialogs.runOnMainThread(
          () ->
              SDLDialog.nSDL_ShowOpenFileDialog(
                  callback, userdata, window, filters, filterCount, defaultLocation, allowMany));
      ci.cancel();
    }
  }

  @Inject(method = "nSDL_ShowSaveFileDialog", at = @At("HEAD"), cancellable = true)
  private static void applefix$saveFileOnMainThread(
      long callback,
      long userdata,
      long window,
      long filters,
      int filterCount,
      long defaultLocation,
      CallbackInfo ci) {
    if (MacFileDialogs.mustMoveToMainThread()) {
      MacFileDialogs.runOnMainThread(
          () ->
              SDLDialog.nSDL_ShowSaveFileDialog(
                  callback, userdata, window, filters, filterCount, defaultLocation));
      ci.cancel();
    }
  }

  @Inject(method = "nSDL_ShowOpenFolderDialog", at = @At("HEAD"), cancellable = true)
  private static void applefix$openFolderOnMainThread(
      long callback,
      long userdata,
      long window,
      long defaultLocation,
      boolean allowMany,
      CallbackInfo ci) {
    if (MacFileDialogs.mustMoveToMainThread()) {
      MacFileDialogs.runOnMainThread(
          () ->
              SDLDialog.nSDL_ShowOpenFolderDialog(
                  callback, userdata, window, defaultLocation, allowMany));
      ci.cancel();
    }
  }

  @Inject(method = "nSDL_ShowFileDialogWithProperties", at = @At("HEAD"), cancellable = true)
  private static void applefix$withPropertiesOnMainThread(
      int type, long callback, long userdata, int properties, CallbackInfo ci) {
    if (MacFileDialogs.mustMoveToMainThread()) {
      MacFileDialogs.runOnMainThread(
          () -> SDLDialog.nSDL_ShowFileDialogWithProperties(type, callback, userdata, properties));
      ci.cancel();
    }
  }
}
