package me.xentany.bcaseschestanimation.particle;

import dev.by1337.bc.animation.AbstractAnimation;
import dev.by1337.bc.particle.ParticleUtil;
import org.bukkit.Particle;
import org.by1337.blib.geom.Vec3d;
import org.jetbrains.annotations.NotNull;

public final class XParticleUtil extends ParticleUtil {

  public static void spawnRing(final @NotNull Vec3d center,
                               final @NotNull AbstractAnimation animation,
                               final @NotNull Particle particle,
                               final double radius,
                               final double step) {
    for (double angle = 0; angle < 2 * Math.PI; angle += step) {
      double x = center.x + radius * Math.cos(angle);
      double z = center.z + radius * Math.sin(angle);
      animation.spawnParticle(particle, x, center.y, z, 0);
    }
  }
}