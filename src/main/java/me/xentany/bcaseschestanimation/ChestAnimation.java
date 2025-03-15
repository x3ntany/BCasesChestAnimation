package me.xentany.bcaseschestanimation;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.by1337.bc.CaseBlock;
import dev.by1337.bc.animation.AbstractAnimation;
import dev.by1337.bc.animation.AnimationContext;
import dev.by1337.bc.particle.ParticleUtil;
import dev.by1337.bc.prize.Prize;
import dev.by1337.bc.prize.PrizeSelector;
import dev.by1337.bc.task.AsyncTask;
import dev.by1337.bc.world.WorldEditor;
import dev.by1337.bc.yaml.CashedYamlContext;
import dev.by1337.virtualentity.api.virtual.VirtualEntity;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Lidded;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.by1337.blib.geom.Vec3d;
import org.by1337.blib.geom.Vec3i;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public final class ChestAnimation extends AbstractAnimation {

  private final Set<Vec3i> placedChestPositions = new HashSet<>();
  private final Prize winner;
  private final Config config;

  private WorldEditor worldEditor;

  private volatile Vec3i selectedChestPosition;
  private volatile boolean waitClick = false;

  public ChestAnimation(final @NotNull CaseBlock caseBlock,
                        final @NotNull AnimationContext context,
                        final @NotNull Runnable onEndCallback,
                        final @NotNull PrizeSelector prizeSelector,
                        final @NotNull CashedYamlContext config,
                        final @NotNull Player player) {
    super(caseBlock, context, onEndCallback, prizeSelector, config, player);

    this.winner = prizeSelector.getRandomPrize();
    this.config = config.get("settings", yamlValue ->
            yamlValue.decode(Config.CODEC)
                    .getOrThrow()
                    .getFirst());
  }

  @Override
  protected void onStart() {
    this.caseBlock.hideHologram();
    this.worldEditor = new WorldEditor(world);
  }

  @Override
  protected void animate() throws InterruptedException {
    var chestConfigurations = this.config.chestConfigurations;

    for (var chestConfiguration : chestConfigurations) {
      var position = this.blockPos.add(chestConfiguration.offset());

      this.worldEditor.setType(position, chestConfiguration.blockData());
      this.placedChestPositions.add(position);
      this.playSound(this.location, Sound.BLOCK_STONE_PLACE, 1, 1);
      this.sleepTicks(3);
    }

    this.sendTitle("", this.config.subtitle, 5, 30, 10);
    this.waitClick = true;
    this.waitUpdate(10_000);

    if (this.selectedChestPosition == null) {
      this.selectedChestPosition = this.placedChestPositions.iterator().next();
    }

    this.openChest(this.selectedChestPosition);
    this.trackEntity(this.winner.createVirtualItem(this.selectedChestPosition.toVec3d().add(0.5, 0.75, 0.5)));

    new AsyncTask() {

      final Vec3d pos = selectedChestPosition.toVec3d().add(0.5, 0, 0.5);

      @Override
      public void run() {
        ParticleUtil.spawnBlockOutlining(this.pos, ChestAnimation.this, Particle.FLAME, 0.1);
      }
    }.timer().delay(6).start(this);

    this.sleepTicks(40);

    chestConfigurations.stream()
            .map(chestConfiguration -> this.blockPos.add(chestConfiguration.offset()))
            .filter(position -> !position.equals(this.selectedChestPosition))
            .forEach(position -> {
              var randomPrize = this.prizeSelector.getRandomPrize();

              this.openChest(position);
              this.trackEntity(randomPrize.createVirtualItem(position.toVec3d().add(0.5, 0.75, 0.5)));
            });

    this.sleepTicks(60);
  }

  @Override
  protected void onEnd() {
    this.placedChestPositions.forEach(position -> {
      var blockCenter = position.toVec3d().add(0.5, 0.5, 0.5);
      var blockType = position.toBlock(this.world).getType();

      this.spawnParticle(Particle.BLOCK_CRACK, blockCenter, 30, blockType.createBlockData());
      this.playSound(blockCenter, Sound.BLOCK_STONE_BREAK, 1, 1);
      this.worldEditor.setType(position, Material.AIR);
    });

    this.placedChestPositions.clear();

    if (this.worldEditor != null) {
      this.worldEditor.close();
    }

    this.caseBlock.showHologram();
    this.caseBlock.givePrize(this.winner, this.player);
  }

  @Override
  protected void onClick(final @NotNull VirtualEntity entity, final @NotNull Player clicker) {}

  @Override
  public void onInteract(final @NotNull PlayerInteractEvent event) {
    if (this.waitClick) {
      var block = event.getClickedBlock();

      if (block == null) {
        return;
      }

      var position = new Vec3i(block);

      if (this.placedChestPositions.contains(position)) {
        event.setCancelled(true);

        this.selectedChestPosition = position;
        this.waitClick = false;
        this.update();
      }
    }
  }

  private void openChest(final @NotNull Vec3i blockPos) {
    this.modifyLidded(blockPos, (block, lidded) -> {
      lidded.open();

      if (block.getType() == Material.CHEST) {
        playSound(Sound.BLOCK_CHEST_OPEN, 1, 1);
      } else {
        playSound(Sound.BLOCK_ENDER_CHEST_OPEN, 1, 1);
      }
    });
  }

  private void modifyLidded(final @NotNull Vec3i blockPos, final @NotNull BiConsumer<Block, Lidded> consumer) {
    this.sync(() -> {
      var block = blockPos.toBlock(this.world);
      var state = block.getState();

      if (state instanceof final Lidded lidded) {
        consumer.accept(block, lidded);
        state.update();
      }
    }).start();
  }

  private record Config(List<ChestConfiguration> chestConfigurations, String subtitle) {

    public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ChestConfiguration.CODEC.listOf()
                            .fieldOf("chests")
                            .forGetter(Config::chestConfigurations),
                    Codec.STRING.fieldOf("subtitle")
                            .forGetter(Config::subtitle)
            ).apply(instance, Config::new)
    );
  }
}