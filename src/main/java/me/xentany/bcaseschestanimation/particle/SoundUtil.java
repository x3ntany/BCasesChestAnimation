package me.xentany.bcaseschestanimation.particle;

import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;

public final class SoundUtil {

  public static @NotNull Sound getSound(final @NotNull String soundName)
      throws NoSuchFieldException, IllegalAccessException {
    try {
      return Sound.valueOf(soundName);
    } catch (final Exception exception) {
      var field = Sound.class.getField(soundName);

      if (Sound.class.isAssignableFrom(field.getType())) {
        return (Sound) field.get(null);
      }
    }

    throw new NoSuchFieldException("Sound not found: " + soundName);
  }
}