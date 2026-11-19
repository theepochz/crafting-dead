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

package com.craftingdead.core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.craftingdead.core.mixin.CapabilityProviderAccessor;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;

/**
 * In creative mode the client is authoritative: {@code handleSetCreativeModeSlot} takes
 * the stack the client sends and writes it straight into the server's inventory slot.
 * That stack travelled through the item stream codec, which only carries data
 * components - capability data (gun attachments, ammo, magazines) is stripped in
 * transit. Writing it over the server's stack silently erases those capabilities.
 *
 * <p>This mixin restores the dropped capability data from the server's current stack
 * whenever the client-sent stack is the same item, so creative-mode inventory edits
 * (which are otherwise a legitimate client authority) cannot wipe gun state.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

  @Inject(method = "handleSetCreativeModeSlot",
      at = @At(value = "INVOKE",
          target = "Lnet/minecraft/world/inventory/InventoryMenu;getSlot(I)Lnet/minecraft/world/inventory/Slot;"))
  private void craftingdead$restoreCapsOnCreativeSlotSet(
      ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
    var self = (ServerGamePacketListenerImpl) (Object) this;
    var player = self.player;
    int slotNum = packet.slotNum();
    ItemStack incoming = packet.itemStack();
    if (incoming == null || incoming.isEmpty() || slotNum < 0) {
      return;
    }
    var menu = player.inventoryMenu;
    if (slotNum >= menu.slots.size()) {
      return;
    }
    ItemStack current = menu.getSlot(slotNum).getItem();
    if (!current.isEmpty() && current.getItem() == incoming.getItem()) {
      HolderLookup.Provider provider = player.level().registryAccess();
      CompoundTag caps = ((CapabilityProviderAccessor) (Object) current)
          .craftingdead$serializeCaps(provider);
      if (caps != null && !caps.isEmpty()) {
        ((CapabilityProviderAccessor) (Object) incoming)
            .craftingdead$deserializeCaps(provider, caps);
        org.slf4j.LoggerFactory.getLogger("CD-DBG").info(
            "creative slot set: restored caps from server stack (slot={})", slotNum);
      }
    }
  }
}
