package me.xentany.bcaseschestanimation;

import dev.by1337.bc.addon.AbstractAddon;
import dev.by1337.bc.animation.AnimationRegistry;
import org.by1337.blib.util.SpacedNameKey;

import java.io.File;

@SuppressWarnings("unused") // That's the way to do it...
public final class BCasesChestAnimation extends AbstractAddon {

  @Override
  protected void onEnable() {
    AnimationRegistry.INSTANCE.register("xcommunity:chest", ChestAnimation::new);

    var file = new File(this.getPlugin().getDataFolder(), "animations/xcommunity/chest.yml");

    this.saveResourceToFile("chest.yml", file);
  }

  @Override
  protected void onDisable() {
    AnimationRegistry.INSTANCE.unregister(new SpacedNameKey("xcommunity:chest"));
  }
}