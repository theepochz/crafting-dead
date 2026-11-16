/*
 * Crafting Dead
 * Copyright (C) 2022  NexusNode LTD
 *
 * This Non-Commercial Software License Agreement (the "Agreement") is made between
 * you (the "Licensee") and NEXUSNODE (BRAD HUNTER). (the "Licensor").
 * By installing or otherwise using Crafting Dead (the "Software"), you agree to be
 * bound by the terms and conditions of this Agreement as may be revised from time
 * to time at Licensor's sole discretion.
 *
 * If you do not agree to the terms and conditions of this Agreement do not download,
 * copy, reproduce or otherwise use any of the source code available online at any time.
 *
 * https://github.com/nexusnode/crafting-dead/blob/1.18.x/LICENSE.txt
 *
 * https://craftingdead.net/terms.php
 */

package com.craftingdead.core.particle;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

public record FlashParticleOptions(float red, float green, float blue, float scale)
    implements ParticleOptions {

  public static final MapCodec<FlashParticleOptions> MAP_CODEC =
      RecordCodecBuilder.mapCodec(instance -> instance
          .group(
              Codec.FLOAT.fieldOf("red").forGetter(FlashParticleOptions::red),
              Codec.FLOAT.fieldOf("green").forGetter(FlashParticleOptions::green),
              Codec.FLOAT.fieldOf("blue").forGetter(FlashParticleOptions::blue),
              Codec.FLOAT.fieldOf("scale").forGetter(FlashParticleOptions::scale))
          .apply(instance, FlashParticleOptions::new));

  public static final StreamCodec<RegistryFriendlyByteBuf, FlashParticleOptions> STREAM_CODEC =
      StreamCodec.composite(
          ByteBufCodecs.FLOAT, FlashParticleOptions::red,
          ByteBufCodecs.FLOAT, FlashParticleOptions::green,
          ByteBufCodecs.FLOAT, FlashParticleOptions::blue,
          ByteBufCodecs.FLOAT, FlashParticleOptions::scale,
          FlashParticleOptions::new);

  public String writeToString() {
    return String.format(java.util.Locale.ROOT, "%s %.2f %.2f %.2f %.2f",
        BuiltInRegistries.PARTICLE_TYPE.getKey(this.getType()), this.red, this.green, this.blue, this.scale);
  }

  @Override
  public ParticleType<FlashParticleOptions> getType() {
    return ModParticleTypes.RGB_FLASH.get();
  }
}
