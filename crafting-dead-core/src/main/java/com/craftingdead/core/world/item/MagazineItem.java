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

package com.craftingdead.core.world.item;

import net.minecraft.network.chat.Component;

import com.craftingdead.core.capability.CapabilityUtil;
import com.craftingdead.core.world.item.gun.magazine.Magazine;
import com.craftingdead.core.world.item.gun.magazine.MagazineImpl;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;

public class MagazineItem extends Item {

  private final float armorPenetration;
  private final int size;

  public MagazineItem(Properties properties) {
    super(properties);
    this.size = properties.size;
    this.armorPenetration = properties.armorPenetration;
  }

  public float getArmorPenetration() {
    return this.armorPenetration;
  }

  public int getSize() {
    return this.size;
  }

  @Override
  public ICapabilityProvider getCapabilityProvider(ItemStack itemStack) {
    return CapabilityUtil.serializableProvider(() -> new MagazineImpl(this), Magazine.CAPABILITY);
  }

  @Override
  public boolean isValidRepairItem(ItemStack itemStack, ItemStack materialStack) {
    return materialStack.is(Tags.Items.GUNPOWDER)
        || super.isValidRepairItem(itemStack, materialStack);
  }

  /**
   * 1.21: item damage is stored on the stack ({@code DataComponents#DAMAGE}); keep these as
   * plain bridge helpers used by the magazine capability.
   */
  public int getMaxDamage(ItemStack itemStack) {
    return this.size;
  }

  public int getDamage(ItemStack itemStack) {
    return itemStack.getDamageValue();
  }

  public void setDamage(ItemStack itemStack, int damage) {
    itemStack.setDamageValue(damage);
  }

  @Override
  public void appendHoverText(ItemStack stack, Item.TooltipContext level, List<Component> lines,
      TooltipFlag tooltipFlag) {
    super.appendHoverText(stack, level, lines, tooltipFlag);

    // Shows the current amount if the maximum size is higher than 1
    if (this.getSize() > 1) {
      int currentAmount =
          stack.getCapability(Magazine.CAPABILITY).map(Magazine::getSize).orElse(0);

      Component amountText = Component.literal(currentAmount + "/" + this.getSize())
          .withStyle(ChatFormatting.RED);

      lines.add(Component.translatable("magazine.amount")
          .withStyle(ChatFormatting.GRAY)
          .append(amountText));
    }

    if (this.armorPenetration > 0) {
      lines.add(Component.translatable("magazine.armor_penetration")
          .withStyle(ChatFormatting.GRAY)
          .append(Component.literal(String.format("%.0f%%", this.armorPenetration))
              .withStyle(ChatFormatting.RED)));
    }
  }

  public static class Properties extends Item.Properties {

    private float armorPenetration;
    private int size;

    public Properties setArmorPenetration(float armorPenetration) {
      this.armorPenetration = armorPenetration;
      return this;
    }

    public Properties setSize(int size) {
      this.size = size;
      return this;
    }
  }
}
