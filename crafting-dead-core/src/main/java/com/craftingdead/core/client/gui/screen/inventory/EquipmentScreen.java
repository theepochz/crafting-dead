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

package com.craftingdead.core.client.gui.screen.inventory;

import net.minecraft.network.chat.Component;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.client.ClientDist;
import com.craftingdead.core.client.gui.widget.button.CompositeButton;
import com.craftingdead.core.network.NetworkChannel;
import com.craftingdead.core.network.message.play.OpenCraftingMenuMessage;
import com.craftingdead.core.network.message.play.OpenStorageMessage;
import com.craftingdead.core.world.inventory.EquipmentMenu;
import com.craftingdead.core.world.item.ModItems;
import com.craftingdead.core.world.item.equipment.Equipment;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.skin.Paint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.ItemStack;

public class EquipmentScreen extends EffectRenderingInventoryScreen<EquipmentMenu> {

  private static final ResourceLocation BACKGROUND =
      ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "textures/gui/container/equipment.png");

  private int oldMouseX;
  private int oldMouseY;

  private Button backpackButton;
  private Button vestButton;

  private boolean transitioning = false;

  public EquipmentScreen(EquipmentMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
  }

  @Override
  public void init() {
    super.init();
    this.vestButton = CompositeButton.button(this.leftPos + 95, this.topPos + 44, 12, 16,
        BACKGROUND)
        .setAtlasPos(196, 224)
        .setHoverAtlasPos(196, 240)
        .setInactiveAtlasPos(183, 240)
        .setAction((button) -> {
          NetworkChannel.PLAY.getSimpleChannel()
              .send(new OpenStorageMessage(Equipment.Slot.VEST), net.minecraftforge.network.PacketDistributor.SERVER.noArg());
          this.transitioning = true;
        }).build();
    this.addRenderableWidget(this.vestButton);
    this.backpackButton = CompositeButton.button(this.leftPos + 95, this.topPos + 62, 12, 16,
        BACKGROUND)
        .setAtlasPos(196, 224)
        .setHoverAtlasPos(196, 240)
        .setInactiveAtlasPos(183, 240)
        .setAction((button) -> {
          NetworkChannel.PLAY.getSimpleChannel()
              .send(new OpenStorageMessage(Equipment.Slot.BACKPACK), net.minecraftforge.network.PacketDistributor.SERVER.noArg());
          this.transitioning = true;
        }).build();
    this.addRenderableWidget(this.backpackButton);
    this.refreshButtonStatus();
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
    super.render(graphics, mouseX, mouseY, partialTicks);
    this.renderTooltip(graphics, mouseX, mouseY);
    this.oldMouseX = mouseX;
    this.oldMouseY = mouseY;
  }

  @Override
  protected void containerTick() {
    super.containerTick();
    this.refreshButtonStatus();
  }

  private void refreshButtonStatus() {
    this.backpackButton.active = this.menu
        .getPlayer()
        .getEquipmentInSlot(Equipment.Slot.BACKPACK)
        .map(MenuConstructor.class::isInstance)
        .orElse(false);
    this.vestButton.active = this.menu
        .getPlayer()
        .getEquipmentInSlot(Equipment.Slot.VEST)
        .map(MenuConstructor.class::isInstance)
        .orElse(false);
  }

  /**
   * If we are waiting for another container GUI to open.
   */
  public boolean isTransitioning() {
    return this.transitioning;
  }

  @Override
  protected void renderLabels(GuiGraphics graphics, int x, int y) {}

  @Override
  protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
    // Note: don't call renderBackground() here - in 1.21+ AbstractContainerScreen
    // delegates renderBackground to renderBg, causing infinite recursion.

    graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

    // Hide the icon of equipment slots if they have an item.
    // Starting at index 35 to skip the player inventory/
    for (int i = 35; i < this.menu.slots.size(); i++) {
      var slot = this.menu.slots.get(i);
      if (slot.hasItem()) {
        graphics.blit(BACKGROUND, slot.x + this.leftPos, slot.y + this.topPos, 8, 141, 16, 16);
      }
    }

    ItemStack gunStack = this.menu.getGunStack();
    gunStack.getCapability(Gun.CAPABILITY).ifPresent(gun -> {

      final int gunSlotX = this.leftPos + 135;
      final int gunSlotY = this.topPos + 26;

      final boolean carriedItemAccepted = gun.isAcceptedAttachment(this.menu.getCarried())
          || Paint.isValid(this.menu.getGunStack(), this.menu.getCarried());

      if ((!this.menu.isCraftingInventoryEmpty() && this.menu.isCraftable())
          || (!this.menu.getCarried().isEmpty() && carriedItemAccepted)) {
        // Green outline
        graphics.blit(BACKGROUND, gunSlotX, gunSlotY, 165, 238, 16, 16);
      } else if (!this.menu.getCarried().isEmpty() && !carriedItemAccepted) {
        // Red outline
        graphics.blit(BACKGROUND, gunSlotX, gunSlotY, 147, 238, 16, 16);
      }
    });

    graphics.blit(BACKGROUND, this.leftPos, this.topPos - 28, 183, 0, 28, 35);
    graphics.blit(BACKGROUND, this.leftPos + 30, this.topPos - 28, 211, 0, 29, 28);
    this.renderFakeItems(graphics);
    this.renderInteractiveTooltips(graphics, mouseX, mouseY);
    this.renderPlayerEntity(graphics);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (this.isMouseOver(this.leftPos + 30, this.topPos - 28, 29, 28, mouseX, mouseY)) {
      if (this.minecraft != null && this.minecraft.player != null) {
        this.minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.2F, 1.0F);
      }
      NetworkChannel.PLAY.getSimpleChannel().send(new OpenCraftingMenuMessage(), net.minecraftforge.network.PacketDistributor.SERVER.noArg());
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (ClientDist.OPEN_EQUIPMENT_MENU.matches(keyCode, scanCode)) {
      this.onClose();
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  /**
   * Renders fake items in the GUI.
   */
  private void renderFakeItems(GuiGraphics graphics) {
    graphics.renderItem(new ItemStack(ModItems.MEDIUM_BLUE_BACKPACK.get()),
        this.leftPos + 6, this.topPos - 20);
    graphics.renderItem(new ItemStack(ModItems.PICKAXE.get()),
        this.leftPos + 36, this.topPos - 22);
  }

  /**
   * Renders tooltips for interactive elements based on mouse position.
   */
  private void renderInteractiveTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
    if (this.isMouseOver(this.leftPos, this.topPos - 28, 29, 28, mouseX, mouseY)) {
      graphics.renderTooltip(this.font, Component.translatable("inventory_inventory.information"),
          mouseX, mouseY);
    }
    if (this.isMouseOver(this.leftPos + 30, this.topPos - 28, 29, 28, mouseX, mouseY)) {
      graphics.renderTooltip(this.font, Component.translatable("inventory_crafting.information"),
          mouseX, mouseY);
    }
  }

  /**
   * Renders the player's 3D model in the crafting screen.
   */
  private void renderPlayerEntity(GuiGraphics graphics) {
    if (this.minecraft != null && this.minecraft.player != null) {
      InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
          this.leftPos + 51, this.topPos + 72, 30,
          (this.leftPos + 51) - this.oldMouseX,
          (this.topPos + 75 - 50) - this.oldMouseY,
          0.0F, 0.0F, 1.0F,
          this.minecraft.player
      );
    }
  }

  /**
   * Checks if the mouse is over a given rectangular area.
   */
  private boolean isMouseOver(int x, int y, int width, int height, double mouseX, double mouseY) {
    return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
  }
}
