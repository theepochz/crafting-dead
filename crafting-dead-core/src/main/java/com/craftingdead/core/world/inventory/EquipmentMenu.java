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

import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.attachment.AttachmentLike;
import com.craftingdead.core.world.item.gun.skin.Paint;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.craftingdead.core.world.item.GunItem;

public class EquipmentMenu extends AbstractContainerMenu {

  private final SimpleContainer craftingContainer = new SimpleContainer(4);

  private final PlayerExtension<?> player;

  /** Set while the view is being rebuilt from the gun, to ignore our own listener callbacks. */
  private boolean updatingView;  public EquipmentMenu(int id, Inventory inventory) {
    this(id, PlayerExtension.getOrThrow(inventory.player));
  }

  public EquipmentMenu(int id, PlayerExtension<?> player) {
    super(ModMenuTypes.EQUIPMENT.get(), id);

    this.player = player;

    this.craftingContainer.addListener(container -> this.applyCraftingContainerToGun());

    var inventory = player.entity().getInventory();

    final int slotSize = 18;

    for (int y = 0; y < 3; ++y) {
      for (int x = 0; x < 9; ++x) {
        this.addSlot(
            new Slot(inventory, x + (y + 1) * 9, 8 + x * slotSize, 84 + y * slotSize));
      }
    }

    for (int x = 0; x < 9; ++x) {
      this.addSlot(new Slot(inventory, x, 8 + x * slotSize, 141));
    }

    int equipmentColumnX = 77;
    int equipmentColumnY = 8;

    this.addSlot(new EquipmentSlot(player,
        Equipment.Slot.HAT, equipmentColumnX, equipmentColumnY));

    this.addSlot(new EquipmentSlot(player,
        Equipment.Slot.CLOTHING,
        equipmentColumnX, equipmentColumnY += slotSize));

    this.addSlot(new EquipmentSlot(player,
        Equipment.Slot.VEST,
        equipmentColumnX, equipmentColumnY += slotSize));

    this.addSlot(new EquipmentSlot(player,
        Equipment.Slot.BACKPACK,
        equipmentColumnX, equipmentColumnY + slotSize));

    int weaponColumnX = 8;
    int weaponColumnY = 8;

    this.addSlot(new EquipmentSlot(player,
        Equipment.Slot.MELEE,
        weaponColumnX, weaponColumnY));

    final int gunCraftSlotGap = 2;
    final int gunCraftY = 6;
    final int gunCraftX = 115;

    // The gun slot IS the crafting interaction point (center of the crafting grid).
    // When the gun leaves the slot (swap, pickup), drop the attachment view right away
    // so the player cannot walk away with the view's copies of the attachments.
    this.addSlot(new GunEquipmentSlot(player,
        gunCraftX + slotSize + gunCraftSlotGap,
        gunCraftY + slotSize + gunCraftSlotGap,
        this::dropAttachmentView));

    final BiPredicate<PredicateSlot, ItemStack> attachmentOrPaintPredicate =
        (slot, itemStack) -> this.getGunStack().getCapability(Gun.CAPABILITY)
            .map(gun -> gun.isAcceptedAttachment(itemStack)).orElse(false);

    final BiPredicate<PredicateSlot, ItemStack> attachmentPredicate =
        (slot, itemStack) -> itemStack.getItem() instanceof AttachmentLike
            && ((AttachmentLike) itemStack.getItem())
                .asAttachment().getInventorySlot().getIndex() == slot.getSlotIndex();

    this.addSlot(new PredicateSlot(this.craftingContainer,
        GunCraftSlotType.MUZZLE_ATTACHMENT.getIndex(),
        gunCraftX,
        gunCraftY + slotSize + gunCraftSlotGap,
        attachmentPredicate.and(attachmentOrPaintPredicate)) {
      @Override
      public void onTake(Player player, ItemStack stack) {
        super.onTake(player, stack);
        // Picking an attachment out of the view is an explicit removal.
        EquipmentMenu.this.removeAttachmentFromGun(stack);
      }
    });

    this.addSlot(new PredicateSlot(this.craftingContainer,
        GunCraftSlotType.UNDERBARREL_ATTACHMENT.getIndex(),
        gunCraftX + slotSize + gunCraftSlotGap,
        gunCraftY + slotSize * 2 + gunCraftSlotGap * 2,
        attachmentPredicate.and(attachmentOrPaintPredicate)) {
      @Override
      public void onTake(Player player, ItemStack stack) {
        super.onTake(player, stack);
        EquipmentMenu.this.removeAttachmentFromGun(stack);
      }
    });
    this.addSlot(new PredicateSlot(this.craftingContainer,
        GunCraftSlotType.OVERBARREL_ATTACHMENT.getIndex(),
        gunCraftX + slotSize + gunCraftSlotGap,
        gunCraftY,
        attachmentPredicate.and(attachmentOrPaintPredicate)) {
      @Override
      public void onTake(Player player, ItemStack stack) {
        super.onTake(player, stack);
        EquipmentMenu.this.removeAttachmentFromGun(stack);
      }
    });

    this.addSlot(new PredicateSlot(this.craftingContainer,
        GunCraftSlotType.PAINT.getIndex(),
        gunCraftX + slotSize * 2 + gunCraftSlotGap * 2,
        gunCraftY + slotSize + gunCraftSlotGap,
        (slot, itemStack) -> Paint.isValid(this.getGunStack(), itemStack)) {
      @Override
      public void onTake(Player player, ItemStack stack) {
        super.onTake(player, stack);
        // Picking the paint out of the view removes it from the gun.
        var gunStack = EquipmentMenu.this.getGunStack();
        gunStack.getCapability(Gun.CAPABILITY)
            .ifPresent(gun -> gun.setPaintStack(ItemStack.EMPTY));
      }
    });

    this.loadEquippedGun();
  }

  public PlayerExtension<?> getPlayer() {
    return this.player;
  }

  @Override
  public boolean stillValid(Player player) {
    return true;
  }

  @Override
  public void broadcastChanges() {
    // The gun's attachment map is the source of truth and lives server-side; the client
    // view is populated by vanilla slot syncing. Rebuild the view only on the server.
    if (!this.player.entity().level().isClientSide()) {
      this.syncCraftingView();
    }
    super.broadcastChanges();
  }

  @Override
  public void removed(Player player) {
    super.removed(player);
    if (!player.level().isClientSide()) {
      // Attachments were applied to the gun the moment they were placed in the view;
      // dropping the view's contents here must not touch the gun (the guard in
      // applyCraftingContainerToGun makes clearContent harmless).
      this.dropAttachmentView();
    }
  }

  public ItemStack getGunStack() {
    return this.player.getItemInSlot(Equipment.Slot.GUN);
  }

  /**
   * Rebuilds the crafting slots to mirror the equipped gun whenever the gun stack
   * changes. The crafting slots are purely a view: the gun's attachment map is the
   * single source of truth. This replaces the old "strip attachments into slots on
   * open, collect them back on close" flow, which lost attachments whenever the world
   * saved while they sat detached in the slots.
   */
  private void syncCraftingView() {
    if (this.updatingView) {
      return;
    }
    this.updatingView = true;
    try {
      this.syncCraftingView0();
    } finally {
      this.updatingView = false;
    }
  }

  private void syncCraftingView0() {
    var gunStack = this.getGunStack();
    var log = org.slf4j.LoggerFactory.getLogger("CD-DBG");
    if (gunStack.isEmpty() || !(gunStack.getItem() instanceof GunItem)) {
      // No gun equipped - the view has nothing to show. Note: clearContent fires the
      // container listener, but applyCraftingContainerToGun ignores empty guns and only
      // ever adds attachments, so this cannot wipe a gun.
      this.craftingContainer.clearContent();
      return;
    }
    gunStack.getCapability(Gun.CAPABILITY).ifPresent(gun -> log
        .info("syncView: server gun atts={}", gun.getAttachments().keySet()));
    gunStack.getCapability(Gun.CAPABILITY).ifPresent(gun -> {
      // Reconcile idempotently: only rewrite a slot if it differs from the gun's state,
      // so the setChanged() cascade never loops or fights the player mid-drag.
      for (var type : GunCraftSlotType.values()) {
        if (type == GunCraftSlotType.PAINT) {
          var paint = gun.getPaintStack();
          if (!ItemStack.isSameItemSameComponents(this.craftingContainer
              .getItem(type.getIndex()), paint)) {
            this.craftingContainer.setItem(type.getIndex(), paint.copy());
          }
        } else {
          var attachment = gun.getAttachments().get(type);
          var expected = attachment == null ? ItemStack.EMPTY : new ItemStack(attachment);
          if (!ItemStack.isSameItemSameComponents(this.craftingContainer
              .getItem(type.getIndex()), expected)) {
            this.craftingContainer.setItem(type.getIndex(), expected);
          }
        }
      }
    });
  }

  /**
   * Drops the attachment view. Used when the gun leaves its slot: the view only mirrors
   * the gun, so it must never outlive it (the items in it are copies, not real items).
   * Safe to call repeatedly - {@code clearContent} on an empty container is a no-op.
   */
  private void dropAttachmentView() {
    this.updatingView = true;
    try {
      this.craftingContainer.clearContent();
    } finally {
      this.updatingView = false;
    }
  }

  /**
   * Explicit removal: the player just picked an attachment out of its view slot, so it
   * comes off the gun. Only fires from real player interactions ({@code onTake}), never
   * from container syncs.
   */
  private void removeAttachmentFromGun(ItemStack attachmentStack) {
    org.slf4j.LoggerFactory.getLogger("CD-DBG")
        .info("onTake: removing {} from gun", attachmentStack.getItem());
    var gunStack = this.getGunStack();
    if (gunStack.isEmpty() || !(attachmentStack.getItem() instanceof AttachmentLike)) {
      return;
    }
    gunStack.getCapability(Gun.CAPABILITY).ifPresent(gun -> {
      var attachment = ((AttachmentLike) attachmentStack.getItem()).asAttachment();
      var attachments = new java.util.HashMap<>(gun.getAttachments());
      if (attachments.remove(attachment.getInventorySlot()) != null) {
        gun.setAttachments(attachments);
      }
    });
  }

  /**
   * Player-driven slot edit: applies the crafting slots' contents to the equipped gun
   * immediately. Attachments can only be added or replaced through this path - never
   * removed. This guard is critical: vanilla also fires the container listener on menu
   * syncs and on {@code clearContent()} when the menu closes, and treating a transiently
   * empty view as "remove everything" would wipe the gun.
   */
  private void applyCraftingContainerToGun() {
    // Ignore callbacks triggered by our own view rebuilds in syncCraftingView.
    if (this.updatingView) {
      return;
    }
    var gunStack = this.getGunStack();
    var log = org.slf4j.LoggerFactory.getLogger("CD-DBG");
    if (gunStack.isEmpty()) {
      log.info("apply: gunStack EMPTY");
      return;
    }
    gunStack.getCapability(Gun.CAPABILITY).ifPresent(gun -> {
      var attachments = new java.util.HashMap<>(gun.getAttachments());
      boolean changed = false;
      for (int i = 0; i < this.craftingContainer.getContainerSize(); i++) {
        var itemStack = this.craftingContainer.getItem(i);
        if (itemStack.isEmpty()) {
          continue;
        }
        boolean accepted = gun.isAcceptedAttachment(itemStack);
        log.info("apply: slot={} item={} accepted={}", i, itemStack.getItem(), accepted);
        if (itemStack.getItem() instanceof AttachmentLike attachmentLike && accepted) {
          var attachment = attachmentLike.asAttachment();
          if (attachments.put(attachment.getInventorySlot(), attachment) != attachment) {
            changed = true;
          }
        } else if (Paint.isValid(gunStack, itemStack)
            && !ItemStack.isSameItemSameComponents(gun.getPaintStack(), itemStack)) {
          log.info("apply: paint -> gun");
          gun.setPaintStack(itemStack);
        }
      }
      if (changed) {
        log.info("apply: setAttachments -> {}", attachments.keySet());
        gun.setAttachments(attachments);
      }
    });
  }

  /**
   * Populates the crafting slots with the equipped gun's attachments so they can be
   * swapped around. The gun keeps its attachments - the slots are only a view, and any
   * edit made through them is applied straight back onto the gun.
   */
  private void loadEquippedGun() {
    this.syncCraftingView();
  }

  public boolean isCraftingInventoryEmpty() {
    return this.craftingContainer.isEmpty();
  }

  public boolean isCraftable() {
    return this.getGunStack().getCapability(Gun.CAPABILITY)
        .map(gun -> {
          for (int i = 0; i < this.craftingContainer.getContainerSize(); i++) {
            ItemStack itemStack = this.craftingContainer.getItem(i);
            if (!itemStack.isEmpty()
                && !gun.isAcceptedAttachment(itemStack)
                && !Paint.isValid(this.getGunStack(), itemStack)) {
              return false;
            }
          }
          return true;
        })
        .orElse(false);
  }

  @Override
  public ItemStack quickMoveStack(Player playerEntity, int clickedIndex) {
    Slot clickedSlot = this.slots.get(clickedIndex);
    if (clickedSlot != null && clickedSlot.hasItem()) {
      // Shift-clicking gun crafting slots is currently unavailable due to
      // the nature of how itemstacks are merged when shift-clicking.
      // In other words, attachments do not get attached.
      if (clickedSlot.container == this.craftingContainer) {
        return ItemStack.EMPTY;
      }

      ItemStack clickedStack = clickedSlot.getItem();

      if (clickedIndex < 36) {
        // Pushes the clicked stack to the higher slots in a "negative direction"
        if (!this.moveItemStackTo(clickedStack, 36, this.slots.size(), true)) {
          return ItemStack.EMPTY;
        }
      } else {
        // Pushes the clicked stack to the lower slots in a "positive direction"
        if (!this.moveItemStackTo(clickedStack, 0, 36, false)) {
          return ItemStack.EMPTY;
        }
      }

      // From vanilla code. Seems to be mandatory.
      if (clickedStack.isEmpty()) {
        clickedSlot.set(ItemStack.EMPTY);
      } else {
        clickedSlot.setChanged();
      }
    }

    return ItemStack.EMPTY;
  }
}
