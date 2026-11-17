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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.item.GunItem;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.attachment.Attachment;
import com.craftingdead.core.world.item.gun.attachment.AttachmentLike;
import com.craftingdead.core.world.item.gun.skin.Paint;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The gun slot of the equipment menu. Operates directly on the player's equipped gun
 * (no result/copy stack) and performs the attachment/paint exchange with the crafting
 * slots:
 * <ul>
 * <li>dropping a gun in -> strip its attachments and paint into the crafting slots</li>
 * <li>taking the gun out -> apply whatever is in the crafting slots back onto it</li>
 * </ul>
 */
public class GunEquipmentSlot extends EquipmentSlot {

  private final SimpleContainer craftingInventory;

  public GunEquipmentSlot(LivingExtension<?, ?> living, int xPosition, int yPosition,
      SimpleContainer craftingInventory) {
    super(living, Equipment.Slot.GUN, xPosition, yPosition);
    this.craftingInventory = craftingInventory;
  }

  @Override
  public boolean mayPlace(ItemStack itemStack) {
    // Guns do not expose the Equipment capability (see GunItem), so the generic
    // EquipmentSlot#mayPlace check would always reject them here.
    return itemStack.getItem() instanceof GunItem;
  }

  @Override
  public void setByPlayer(ItemStack itemStack, ItemStack previous) {
    // Only strip when the player genuinely places a DIFFERENT gun in; never on
    // synchronization replays of the same stack.
    if (!itemStack.isEmpty()
        && (previous.isEmpty()
            || !net.minecraft.world.item.ItemStack.isSameItemSameComponents(previous, itemStack))) {
      itemStack.getCapability(Gun.CAPABILITY).ifPresent(this::stripInto);
    }
    super.setByPlayer(itemStack, previous);
  }

  @Override
  public void onTake(Player player, ItemStack stack) {
    // The gun leaves the slot: apply whatever is in the crafting slots back onto it.
    stack.getCapability(Gun.CAPABILITY)
        .ifPresent(this::collectInto);
    super.onTake(player, stack);
  }

  /**
   * Applies the crafting slots' contents (attachments/paint) onto the given gun and
   * clears the crafting slots.
   */
  private void collectInto(Gun gun) {
    Map<GunCraftSlotType, Attachment> attachments = new HashMap<>();
    for (int i = 0; i < this.craftingInventory.getContainerSize(); i++) {
      ItemStack itemStack = this.craftingInventory.getItem(i);
      if (itemStack.isEmpty()) {
        continue;
      }
      if (itemStack.getItem() instanceof AttachmentLike attachmentLike
          && gun.isAcceptedAttachment(itemStack)) {
        var attachment = attachmentLike.asAttachment();
        attachments.put(attachment.getInventorySlot(), attachment);
        this.craftingInventory.setItem(i, ItemStack.EMPTY);
      } else if (Paint.isValid(gun.getItemStack(), itemStack)) {
        gun.setPaintStack(itemStack);
        this.craftingInventory.setItem(i, ItemStack.EMPTY);
      }
    }
    gun.setAttachments(attachments);
  }

  /**
   * Strips the gun's attachments and paint into the crafting slots (called when a gun
   * enters the slot).
   */
  private void stripInto(Gun gun) {
    gun.getAttachments().forEach((type, attachment) -> this.craftingInventory
        .setItem(type.getIndex(), new ItemStack(attachment)));
    gun.setAttachments(Collections.emptyMap());

    if (!gun.getPaintStack().isEmpty()) {
      this.craftingInventory.setItem(GunCraftSlotType.PAINT.getIndex(), gun.getPaintStack());
      gun.setPaintStack(ItemStack.EMPTY);
    }
  }

  /**
   * Only called by menu synchronization (initializeContents, setItem) - never by direct
   * player interaction in 1.21. Must NOT do attachment bookkeeping, otherwise opening the
   * menu would strip the equipped gun's attachments.
   */
  @Override
  public void set(ItemStack itemStack) {
    super.set(itemStack);
  }
}
