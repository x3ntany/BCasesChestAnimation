package me.xentany.bcaseschestanimation.animation;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.by1337.blib.geom.Vec3i;
import org.jetbrains.annotations.NotNull;

public final class ChestAnimationListener implements Listener {

  @EventHandler
  public void onInteract(final @NotNull PlayerInteractEvent event) {
    var placedChestPositions = ChestAnimation.PLACED_CHEST_POSITIONS;

    if (placedChestPositions.isEmpty()) {
      return;
    }

    var block = event.getClickedBlock();

    if (block == null) {
      return;
    }

    var position = new Vec3i(block);

    if (placedChestPositions.values().stream().anyMatch(positions ->
        positions.contains(position))) {
      event.setCancelled(true);
    }
  }
}