package me.xentany.bcaseschestanimation.serialization;

import blib.com.mojang.serialization.DataResult;
import blib.com.mojang.serialization.DynamicOps;
import blib.com.mojang.serialization.codecs.PrimitiveCodec;
import org.bukkit.Particle;
import org.by1337.blib.configuration.serialization.BukkitCodecs;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class XBukkitCodecs extends BukkitCodecs {

  public static final PrimitiveCodec<Particle> PARTICLE = new PrimitiveCodec<>() {
    public <T> DataResult<Particle> read(@NotNull DynamicOps<T> ops, T t) {
      return ops.getStringValue(t).flatMap(particleName -> {
        try {
          var particle = Particle.valueOf(particleName.toUpperCase(Locale.ROOT));
          return DataResult.success(particle);
        } catch (final IllegalArgumentException e) {
          return DataResult.error(() -> "Invalid particle name: " + particleName);
        }
      });
    }

    public <T> T write(final @NotNull DynamicOps<T> ops, final @NotNull Particle particle) {
      return ops.createString(particle.name());
    }
  };
}