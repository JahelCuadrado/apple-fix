package com.einsteinmods.applefix.mixin;

import com.einsteinmods.applefix.exit.LeftoverThreads;
import net.minecraft.client.main.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Quitting no longer hangs on threads mods left behind; see LeftoverThreads.
 *
 * <p>{@code Main#main} ends with {@code minecraft.run()}, {@code minecraft.exitWorldAndClose()} and
 * {@code ClientShutdownWatchdog.startShutdownWatchdog("post-main", true, ...)}; its last return
 * (TAIL) is only reached after that normal shutdown (error paths return earlier, after {@code
 * Minecraft#crash}). Vanilla's watchdog is already running there and stays as the fallback.
 */
@Mixin(Main.class)
abstract class MainMixin {
  @Inject(method = "main", at = @At("TAIL"))
  private static void applefix$endIfOnlyIdleThreadsRemain(String[] args, CallbackInfo info) {
    LeftoverThreads.endIfOnlyIdleThreadsRemain();
  }
}
