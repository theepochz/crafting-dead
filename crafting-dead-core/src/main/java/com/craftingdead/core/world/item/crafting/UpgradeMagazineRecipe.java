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

package com.craftingdead.core.world.item.crafting;

import com.craftingdead.core.capability.CapabilityUtil;
import com.craftingdead.core.world.item.gun.magazine.Magazine;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;

public class UpgradeMagazineRecipe extends CustomRecipe {

  private static final int MIDDLE_SLOT_INDEX = 4;

  public static final MapCodec<UpgradeMagazineRecipe> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
          Ingredient.CODEC_NONEMPTY.fieldOf("magazine")
              .forGetter(recipe -> recipe.magazine),
          ItemStack.CODEC.fieldOf("nextTier")
              .forGetter(recipe -> recipe.nextTier))
          .apply(instance, UpgradeMagazineRecipe::new));

  public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeMagazineRecipe> STREAM_CODEC =
      StreamCodec.composite(
          Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.magazine,
          ItemStack.STREAM_CODEC, recipe -> recipe.nextTier,
          UpgradeMagazineRecipe::new);

  private final Ingredient magazine;
  private final ItemStack nextTier;

  public UpgradeMagazineRecipe(Ingredient magazine, ItemStack nextTier) {
    super(CraftingBookCategory.MISC);
    this.magazine = magazine;
    this.nextTier = nextTier;
  }

  @Override
  public boolean matches(CraftingInput inventory, Level world) {
    for (int i = 0; i < inventory.size(); ++i) {
      switch (i) {
        case MIDDLE_SLOT_INDEX: // Middle slot
          if (!this.magazine.test(inventory.getItem(i))) {
            return false;
          }
          break;
        default: // All other slots
          if (!inventory.getItem(i).is(Tags.Items.NUGGETS_IRON)) {
            return false;
          }
          break;
      }
    }
    return true;
  }

  @Override
  public ItemStack assemble(CraftingInput inventory, HolderLookup.Provider registries) {
    Magazine magazine = CapabilityUtil.getOrThrow(
        Magazine.CAPABILITY, inventory.getItem(MIDDLE_SLOT_INDEX), Magazine.class);

    CapabilityUtil.getOrThrow(
        Magazine.CAPABILITY, this.nextTier, Magazine.class).setSize(magazine.getSize());

    return this.nextTier;
  }

  @Override
  public boolean canCraftInDimensions(int width, int height) {
    return width * height == 9;
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return ModRecipeSerializers.UPGRADE_MAGAZINE.get();
  }

  public static class Serializer implements RecipeSerializer<UpgradeMagazineRecipe> {

    @Override
    public MapCodec<UpgradeMagazineRecipe> codec() {
      return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, UpgradeMagazineRecipe> streamCodec() {
      return STREAM_CODEC;
    }
  }
}
