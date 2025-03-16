package me.xentany.bcaseschestanimation;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.by1337.bc.CaseBlock;
import dev.by1337.bc.animation.AbstractAnimation;
import dev.by1337.bc.animation.AnimationContext;
import dev.by1337.bc.prize.Prize;
import dev.by1337.bc.prize.PrizeSelector;
import dev.by1337.bc.task.AsyncTask;
import dev.by1337.bc.world.WorldEditor;
import dev.by1337.bc.yaml.CashedYamlContext;
import dev.by1337.virtualentity.api.virtual.VirtualEntity;
import me.xentany.bcaseschestanimation.serialization.ChestParameters;
import me.xentany.bcaseschestanimation.particle.XParticleUtil;
import me.xentany.bcaseschestanimation.serialization.XBukkitCodecs;
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
    if (this.config.shouldSpawnRing) {
      new AsyncTask() {

        final Vec3d position = blockPos.toVec3d().add(config.ringOffset);
        final Particle particle = config.ringParticle;
        final double radius = config.ringRadius;
        final double step = config.ringStep;

        @Override
        public void run() {
          XParticleUtil.spawnRing(position, ChestAnimation.this,
                  particle == null ? Particle.FLAME : particle, radius, step);
        }
      }.timer().delay(6).start(this);
    }

    var chestsParameters = this.config.chestParameters;

    for (var chestParameters : chestsParameters) {
      var position = this.blockPos.add(chestParameters.offset());

      this.worldEditor.setType(position, chestParameters.blockData());
      this.placedChestPositions.add(position);

      var sound = this.config.spawnSound;

      this.playSound(this.location, sound == null ? Sound.BLOCK_STONE_PLACE : sound, 1, 1);
      this.sleep(this.config.spawnDelay);
    }

    this.sendTitle("", this.config.subtitle, 5, 30, 10);
    this.waitClick = true;
    this.waitUpdate(this.config.clickWait);

    if (this.selectedChestPosition == null) {
      this.selectedChestPosition = this.placedChestPositions.iterator().next();
    }

    this.openChest(this.selectedChestPosition);
    this.trackEntity(this.winner.createVirtualItem(this.selectedChestPosition.toVec3d().add(0.5, 0.75, 0.5)));

    if (this.config.shouldSpawnBlockOutlining) {
      new AsyncTask() {

        final Vec3d position = selectedChestPosition.toVec3d().add(0.5, 0, 0.5);
        final Particle particle = config.blockOutliningParticle;
        final double step = config.blockOutliningStep;

        @Override
        public void run() {
          XParticleUtil.spawnBlockOutlining(this.position, ChestAnimation.this,
                  particle == null ? Particle.FLAME : particle, step);
        }
      }.timer().delay(6).start(this);
    }

    this.sleep(this.config.idle);

    chestsParameters.stream()
            .map(chestParameters -> this.blockPos.add(chestParameters.offset()))
            .filter(position -> !position.equals(this.selectedChestPosition))
            .forEach(position -> {
              var randomPrize = this.prizeSelector.getRandomPrize();

              this.openChest(position);
              this.trackEntity(randomPrize.createVirtualItem(position.toVec3d().add(0.5, 0.75, 0.5)));
            });

    this.sleep(this.config.beforeEnd);
  }

  @Override
  protected void onEnd() {
    this.placedChestPositions.forEach(position -> {
      var blockCenter = position.toVec3d().add(0.5, 0.5, 0.5);
      var blockType = position.toBlock(this.world).getType();

      this.spawnParticle(Particle.BLOCK_CRACK, blockCenter, 30, blockType.createBlockData());

      var sound = this.config.spawnSound;

      this.playSound(blockCenter, sound == null ? Sound.BLOCK_STONE_BREAK : sound, 1, 1);
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
    var block = event.getClickedBlock();

    if (block == null) {
      return;
    }

    var position = new Vec3i(block);

    if (this.placedChestPositions.contains(position)) {
      event.setCancelled(true);

      if (waitClick) {
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
        this.playSound(Sound.BLOCK_CHEST_OPEN, 1, 1);
      } else {
        this.playSound(Sound.BLOCK_ENDER_CHEST_OPEN, 1, 1);
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

  private record Config(boolean shouldSpawnRing,
                        Vec3d ringOffset,
                        Particle ringParticle,
                        double ringRadius,
                        double ringStep,
                        boolean shouldSpawnBlockOutlining,
                        Particle blockOutliningParticle,
                        double blockOutliningStep,
                        Sound spawnSound,
                        long spawnDelay,
                        long clickWait,
                        long idle,
                        long beforeEnd,
                        Sound endSound,
                        String subtitle,
                        List<ChestParameters> chestParameters) {

    public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("should-spawn-ring")
                            .forGetter(Config::shouldSpawnRing),
                    Vec3d.CODEC.fieldOf("ring-offset")
                            .forGetter(Config::ringOffset),
                    XBukkitCodecs.PARTICLE.fieldOf("ring-particle")
                            .forGetter(Config::ringParticle),
                    Codec.DOUBLE.fieldOf("ring-radius")
                            .forGetter(Config::ringRadius),
                    Codec.DOUBLE.fieldOf("ring-step")
                            .forGetter(Config::ringStep),
                    Codec.BOOL.fieldOf("should-spawn-block-outlining")
                            .forGetter(Config::shouldSpawnBlockOutlining),
                    XBukkitCodecs.PARTICLE.fieldOf("block-outlining-particle")
                            .forGetter(Config::ringParticle),
                    Codec.DOUBLE.fieldOf("block-outlining-step")
                            .forGetter(Config::blockOutliningStep),
                    XBukkitCodecs.SOUND.fieldOf("spawn-sound")
                            .forGetter(Config::spawnSound),
                    Codec.LONG.fieldOf("spawn-delay")
                            .forGetter(Config::spawnDelay),
                    Codec.LONG.fieldOf("click-wait")
                            .forGetter(Config::clickWait),
                    Codec.LONG.fieldOf("idle")
                            .forGetter(Config::idle),
                    Codec.LONG.fieldOf("before-end")
                            .forGetter(Config::beforeEnd),
                    XBukkitCodecs.SOUND.fieldOf("end-sound")
                            .forGetter(Config::endSound),
                    Codec.STRING.fieldOf("subtitle")
                            .forGetter(Config::subtitle),
                    ChestParameters.CODEC.listOf()
                            .fieldOf("chests")
                            .forGetter(Config::chestParameters)
            ).apply(instance, Config::new)
    );
  }
}