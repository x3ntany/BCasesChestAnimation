package me.xentany.bcaseschestanimation;

import blib.com.mojang.serialization.Codec;
import blib.com.mojang.serialization.codecs.RecordCodecBuilder;
import org.bukkit.block.data.BlockData;
import org.by1337.blib.configuration.serialization.BukkitCodecs;
import org.by1337.blib.geom.Vec3i;

public record ChestConfiguration(Vec3i offset, BlockData blockData) {

  public static final Codec<ChestConfiguration> CODEC = RecordCodecBuilder.create(instance ->
          instance.group(
                  Vec3i.CODEC.fieldOf("offset").forGetter(ChestConfiguration::offset),
                  BukkitCodecs.BLOCK_DATA.fieldOf("block-data").forGetter(ChestConfiguration::blockData)
          ).apply(instance, ChestConfiguration::new)
  );
}