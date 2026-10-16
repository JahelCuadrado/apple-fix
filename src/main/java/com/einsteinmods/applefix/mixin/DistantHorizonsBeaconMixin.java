package com.einsteinmods.applefix.mixin;

import com.einsteinmods.applefix.distanthorizons.DistantHorizonsExitWait;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Distant Horizons exit freeze: the pause after a beacon refresh ends once DH's file pool is shut
 * down; see DistantHorizonsExitWait.
 *
 * <p>DH 3.3.4 {@code LodQuadTree#tryRefreshRenderingBeaconsAsync} queues a lambda on the file pool
 * that refreshes beacons, then calls {@code Thread.sleep(2000)} (inside a {@code catch
 * (InterruptedException)}) before completing its future, to pace refreshes. The lambda name is
 * compiler-generated: {@code @Pseudo} and {@code require = 0} skip it without DH or once renamed.
 */
@Pseudo
@Mixin(targets = "com.seibel.distanthorizons.core.render.QuadTree.LodQuadTree")
abstract class DistantHorizonsBeaconMixin {
  @WrapOperation(
      method = "lambda$tryRefreshRenderingBeaconsAsync$0",
      at = @At(value = "INVOKE", target = "Ljava/lang/Thread;sleep(J)V"),
      require = 0)
  private void applefix$endBeaconPauseOnShutdown(long millis, Operation<Void> original)
      throws InterruptedException {
    DistantHorizonsExitWait.beaconRefreshPause(millis);
  }
}
