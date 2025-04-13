package me.xentany.bcaseschestanimation;

import dev.by1337.bc.addon.AbstractAddon;
import dev.by1337.bc.animation.AnimationRegistry;
import me.xentany.bcaseschestanimation.animation.ChestAnimation;
import me.xentany.bcaseschestanimation.animation.ChestAnimationListener;
import me.xentany.bcaseschestanimation.particle.SafeEnumUtil;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.by1337.blib.util.SpacedNameKey;

import java.io.File;

@SuppressWarnings("unused") // That's the way to do it...
public final class BCasesChestAnimation extends AbstractAddon {

  private ChestAnimationListener animationListener;

  @Override
  protected void onEnable() {
    var file = new File(this.getPlugin().getDataFolder(), "animations/xcommunity/chest.yml");

    this.saveResourceToFile("chest.yml", file);

    SafeEnumUtil.loadLogger(this.getLogger());

    AnimationRegistry.INSTANCE.register("xcommunity:chest", ChestAnimation::new);

    this.animationListener = new ChestAnimationListener();

    Bukkit.getPluginManager().registerEvents(this.animationListener, this.getPlugin());
  }

  @Override
  protected void onDisable() {
    AnimationRegistry.INSTANCE.unregister(new SpacedNameKey("xcommunity:chest"));
    HandlerList.unregisterAll(this.animationListener);
  }
}