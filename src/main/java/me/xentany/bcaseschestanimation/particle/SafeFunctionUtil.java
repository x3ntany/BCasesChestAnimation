package me.xentany.bcaseschestanimation.particle;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public final class SafeFunctionUtil {

  private static Logger logger;

  public static void loadLogger(final @NotNull Logger logger) {
    if (SafeFunctionUtil.logger == null) {
      SafeFunctionUtil.logger = logger;
    }
  }

  @FunctionalInterface
  public interface SafeValueOf<T> {
    T apply(final @NotNull String name) throws Exception;
  }

  public static <T> @NotNull T getOrFallback(final @NotNull String name,
                                             final @NotNull SafeValueOf<T> function,
                                             final @NotNull T fallback) {
    try {
      return function.apply(name);
    } catch (final Exception exception) {
      logger.error("Invalid value for {}: {}", name, exception.getMessage());
      return fallback;
    }
  }
}