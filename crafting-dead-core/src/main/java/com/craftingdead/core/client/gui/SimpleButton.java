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

package com.craftingdead.core.client.gui;

import net.minecraft.network.chat.Component;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.util.Mth;

public class SimpleButton extends Button {

  private int backgroundColour = 0x333333;

  public SimpleButton(int x, int y, int width, int height, Component text, OnPress action) {
    super(Button.builder(text, action).bounds(x, y, width, height));
    this.setFGColor(0xE3BE2B);
  }

  public void setBackgroundColour(int backgroundColour) {
    this.backgroundColour = backgroundColour;
  }

  @Override
  protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY,
      float partialTicks) {
    final Minecraft minecraft = Minecraft.getInstance();
    final int x = this.getX();
    final int y = this.getY();
    guiGraphics.fill(x, y, x + this.width + 1, y + this.height + 1, 0x33000000);
    guiGraphics.fill(x, y, x + this.width, y + this.height,
        this.backgroundColour | Mth.ceil(this.alpha * 255.0F) << 24);
    guiGraphics.drawCenteredString(minecraft.font, this.getMessage(),
        x + this.width / 2, y + (this.height - 8) / 2,
        (this.active ? this.isHovered ? this.packedFGColor : 0xFFFFFF : 0xA0A0A0)
            | Mth.ceil(this.alpha * 255.0F) << 24);
  }
}
