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
import com.craftingdead.core.world.item.GunItem;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.attachment.Attachment;
import com.craftingdead.core.world.item.gun.attachment.AttachmentLike;
import com.craftingdead.core.world.item.gun.skin.Paint;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The gun slot that the player interacts with in the equipment menu. All attachment/paint
 * bookkeeping happens against the REAL equipped stack - no copies, no result slot.
 */
public class GunCraftSlot extends Slot {

  private final SimpleContainer craftingInventory;

  public GunCraftSlot(ResultContainer inventory, int slotIndex, int x, int y,
      SimpleContainer craftingInventory) {
    super(inventory, slotIndex, x, y);
    this.craftingInventory = craftingInventory;
  }

  @Override
  public boolean mayPlace(ItemStack itemStack) {
    return itemStack.getItem() instanceof GunItem;
  }

  /**
   * Server-authoritative path: every player interaction (pick up, swap, place) ends up here.
   * When a gun is dropped into the slot, its attachments and paint are stripped into the
   * crafting slots so they can be swapped around.
   */
  @Override
  public void setByPlayer(ItemStack itemStack, ItemStack previous) {
    itemStack.getCapability(Gun.CAPABILITY).ifPresent(gun -> {
      gun.getAttachments().forEach((type, attachment) -> this.craftingInventory
          .setItem(type.getIndex(), new ItemStack(attachment)));
      gun.setAttachments(Collections.emptyMap());

      if (!gun.getPaintStack().isEmpty()) {
        this.craftingInventory.setItem(GunCraftSlotType.PAINT.getIndex(), gun.getPaintStack());
        gun.setPaintStack(ItemStack.EMPTY);
      }
    });
    super.setByPlayer(itemStack, previous);
  }

  @Override
  public void onTake(Player playerEntity, ItemStack gunStack) {
    // Gathers whatever is sitting in the crafting slots back onto the gun.
    gunStack.getCapability(Gun.CAPABILITY).ifPresent(gun -> {
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
        } else if (Paint.isValid(gunStack, itemStack)) {
          gun.setPaintStack(itemStack);
          this.craftingInventory.setItem(i, ItemStack.EMPTY);
        }
      }
      gun.setAttachments(attachments);
    });
    super.onTake(playerEntity, gunStack);
  }
}
