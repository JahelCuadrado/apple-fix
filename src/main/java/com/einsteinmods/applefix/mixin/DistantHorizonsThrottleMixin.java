package com.einsteinmods.applefix.mixin;

import com.einsteinmods.applefix.distanthorizons.DistantHorizonsExitWait;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.concurrent.ThreadPoolExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Distant Horizons exit freeze: DH's throttle sleep ends once its pool is shut down; see
 * DistantHorizonsExitWait.
 *
 * <p>DH 3.3.4 {@code RateLimitedThreadPoolExecutor#afterExecute} calls {@code Thread.sleep} once,
 * inside a {@code catch (InterruptedException)}. {@code @Pseudo} and {@code require = 0}: skipped
 * without DH or if that call is gone.
 */
@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.util.threading.RateLimitedThreadPoolExecutor")
abstract class DistantHorizonsThrottleMixin {
  @WrapOperation(
      method = "afterExecute",
      at = @At(value = "INVOKE", target = "Ljava/lang/Thread;sleep(J)V"),
      require = 0)
  private void applefix$endThrottleOnShutdown(long millis, Operation<Void> original)
      throws InterruptedException {
    if ((Object) this instanceof ThreadPoolExecutor pool) {
      DistantHorizonsExitWait.throttleSleep(pool, millis);
    } else {
      original.call(millis);
    }
  }
}
