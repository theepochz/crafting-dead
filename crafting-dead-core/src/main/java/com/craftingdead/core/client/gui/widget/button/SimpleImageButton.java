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

package com.craftingdead.core.client.gui.widget.button;

import net.minecraft.network.chat.Component;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceLocation;

public class SimpleImageButton extends Button {

  private ResourceLocation textureLocation;

  public SimpleImageButton(int x, int y, int width, int height,
      ResourceLocation textureLocation, Component text, Button.OnPress actionListener) {
    super(Button.builder(text, actionListener).bounds(x, y, width, height));
    this.textureLocation = textureLocation;
  }

  @Override
  protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY,
      float partialTicks) {
    final int x = this.getX();
    final int y = this.getY();
    guiGraphics.blit(this.textureLocation, x, y, 0.0F, 0.0F, this.width, this.height, this.width,
        this.height);
    if (this.isHoveredOrFocused()) {
      final int opacity = Math.min((int) (this.alpha * 0.5F * 255.0F), 255);
      guiGraphics.fill(x, y, x + this.width, y + this.height, 0xFFFFFF + (opacity << 24));
    }
  }
}
