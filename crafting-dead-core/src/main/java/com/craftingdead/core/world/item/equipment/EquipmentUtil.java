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

package com.craftingdead.core.world.item.equipment;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class EquipmentUtil {

  private EquipmentUtil() {}

  /**
   * Converts a plain-attribute multimap into the holder-keyed form expected by vanilla's
   * {@code AttributeMap} APIs.
   */
  public static Multimap<Holder<Attribute>, AttributeModifier> toHolderMap(
      Multimap<Attribute, AttributeModifier> modifiers) {
    ImmutableMultimap.Builder<Holder<Attribute>, AttributeModifier> builder =
        ImmutableMultimap.builder();
    for (var entry : modifiers.entries()) {
      builder.put(holderOf(entry.getKey()), entry.getValue());
    }
    return builder.build();
  }

  private static Holder<Attribute> holderOf(Attribute attribute) {
    ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attribute);
    return BuiltInRegistries.ATTRIBUTE.getHolderOrThrow(
        net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.ATTRIBUTE, id));
  }
}
