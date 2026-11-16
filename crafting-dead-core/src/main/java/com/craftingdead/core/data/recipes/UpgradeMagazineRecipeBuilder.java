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

package com.craftingdead.core.data.recipes;

import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.Optional;
import com.craftingdead.core.world.item.crafting.UpgradeMagazineRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class UpgradeMagazineRecipeBuilder implements RecipeBuilder {

  /**
   * Minimal holder for the vanilla root recipe advancement, used as the parent so that we don't
   * rely on the deprecated {@link Advancement.Builder#parent(ResourceLocation)} overload. Only the
   * holder's id is consumed by the builder.
   */
  private static final AdvancementHolder ROOT_RECIPE_ADVANCEMENT_HOLDER = new AdvancementHolder(
      ROOT_RECIPE_ADVANCEMENT,
      new Advancement(Optional.of(ROOT_RECIPE_ADVANCEMENT), Optional.empty(),
          AdvancementRewards.EMPTY, Map.of(), AdvancementRequirements.EMPTY, false));

  private final Ingredient magazine;
  private final Item nextTier;
  private final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();
  @Nullable
  private String group;

  private UpgradeMagazineRecipeBuilder(Ingredient magazine, Item nextTier) {
    this.magazine = magazine;
    this.nextTier = nextTier;
  }

  public static UpgradeMagazineRecipeBuilder create(Ingredient magazine, Item nextTier) {
    return new UpgradeMagazineRecipeBuilder(magazine, nextTier);
  }

  @Override
  public UpgradeMagazineRecipeBuilder unlockedBy(String id, Criterion<?> criterion) {
    this.advancement.addCriterion(id, criterion);
    return this;
  }

  @Override
  public UpgradeMagazineRecipeBuilder group(@Nullable String group) {
    this.group = group;
    return this;
  }

  @Override
  public Item getResult() {
    return this.nextTier;
  }

  @Override
  public void save(RecipeOutput output, ResourceLocation id) {
    this.advancement.parent(ROOT_RECIPE_ADVANCEMENT_HOLDER)
        .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
        .rewards(AdvancementRewards.Builder.recipe(id))
        .requirements(AdvancementRequirements.Strategy.OR);
    AdvancementHolder advancementHolder = this.advancement.build(id.withPrefix("recipes/misc/"));
    output.accept(id,
        new UpgradeMagazineRecipe(this.magazine, new ItemStack(this.nextTier)),
        advancementHolder);
  }
}
