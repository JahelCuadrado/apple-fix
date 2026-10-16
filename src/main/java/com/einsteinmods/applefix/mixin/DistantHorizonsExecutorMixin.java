package com.einsteinmods.applefix.mixin;

import com.einsteinmods.applefix.distanthorizons.DistantHorizonsExitWait;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Distant Horizons exit freeze; see DistantHorizonsExitWait.
 *
 * <p>{@code @Pseudo} and {@code require = 0}: skipped without DH, or if DH 3.3.4's {@code
 * PriorityTaskPicker$Executor#awaitTermination(long, TimeUnit)} (called by {@code
 * PriorityTaskPicker#shutdownNow} with 5 s) is gone. Other threads keep DH's own wait.
 */
@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.util.threading.PriorityTaskPicker$Executor")
abstract class DistantHorizonsExecutorMixin {
  @Inject(method = "awaitTermination", at = @At("HEAD"), cancellable = true, require = 0)
  private void applefix$runRenderTasksWhileWaiting(
      long timeout, TimeUnit unit, CallbackInfoReturnable<Boolean> cir)
      throws InterruptedException {
    if ((Object) this instanceof ExecutorService pool
        && DistantHorizonsExitWait.mustRunRenderTasks()) {
      cir.setReturnValue(DistantHorizonsExitWait.awaitTermination(pool, timeout, unit));
    }
  }
}
