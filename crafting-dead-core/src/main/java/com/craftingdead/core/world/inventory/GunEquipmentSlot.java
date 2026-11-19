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

package com.craftingdead.core.world.inventory;

import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.item.GunItem;
import com.craftingdead.core.world.item.equipment.Equipment;
import net.minecraft.world.item.ItemStack;

/**
 * The gun slot of the equipment menu. It operates directly on the player's equipped gun
 * stack - no copies are made, so there is nothing that could be duplicated or dropped.
 *
 * <p>Attachments are never moved off the gun: the crafting slots in {@link EquipmentMenu}
 * are only a view whose edits are applied to the gun immediately.
 */
public class GunEquipmentSlot extends EquipmentSlot {

  private final Runnable onGunRemoved;

  public GunEquipmentSlot(LivingExtension<?, ?> living, int xPosition, int yPosition,
      Runnable onGunRemoved) {
    super(living, Equipment.Slot.GUN, xPosition, yPosition);
    this.onGunRemoved = onGunRemoved;
  }

  @Override
  public boolean mayPlace(ItemStack itemStack) {
    // Guns do not expose the Equipment capability (see GunItem), so the generic
    // EquipmentSlot#mayPlace check would always reject them here.
    return itemStack.getItem() instanceof GunItem;
  }

  /**
   * Called by vanilla only for genuine player interactions - menu synchronisation goes
   * through {@link #set(ItemStack)} instead. If a gun is swapped out (or removed) the
   * attachment view must be dropped immediately, otherwise the player could pick up the
   * view's copies while the gun is no longer equipped.
   */
  @Override
  public void setByPlayer(ItemStack stack, ItemStack previousStack) {
    super.setByPlayer(stack, previousStack);
    if (previousStack.getItem() instanceof GunItem && !(stack.getItem() instanceof GunItem)) {
      this.onGunRemoved.run();
    }
  }

  @Override
  public ItemStack remove(int amount) {
    var previousStack = this.getItem();
    var removed = super.remove(amount);
    if (previousStack.getItem() instanceof GunItem && this.getItem().isEmpty()) {
      this.onGunRemoved.run();
    }
    return removed;
  }
}
