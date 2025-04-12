package me.xentany.bcaseschestanimation.particle;

import dev.by1337.bc.animation.AbstractAnimation;
import dev.by1337.bc.particle.ParticleUtil;
import org.bukkit.Particle;
import org.by1337.blib.geom.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.stream.DoubleStream;

public final class XParticleUtil extends ParticleUtil {

  public static void spawnRing(final @NotNull Vec3d center,
                               final @NotNull AbstractAnimation animation,
                               final @NotNull Particle particle,
                               final double radius,
                               final double step) {
    DoubleStream.iterate(0, angle -> angle < 2 * Math.PI, angle -> angle + step)
        .forEach(angle -> {
      var x = center.x + radius * Math.cos(angle);
      var z = center.z + radius * Math.sin(angle);

      animation.spawnParticle(particle, x, center.y, z, 0);
    });
  }
}