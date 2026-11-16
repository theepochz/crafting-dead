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

package com.craftingdead.core.world.item.enchantment;

import com.craftingdead.core.CraftingDead;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Enchantments are data driven in 1.21: definitions live in
 * {@code data/craftingdead/enchantment/*.json}. Only the resource keys are declared here.
 */
public class ModEnchantments {

  public static final ResourceKey<Enchantment> ARMOR_PENETRATION =
      ResourceKey.create(Registries.ENCHANTMENT,
          ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "armor_penetration"));

  public static Holder<Enchantment> holder(Level level) {
    return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
        .getHolderOrThrow(ARMOR_PENETRATION);
  }
}
