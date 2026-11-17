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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.craftingdead.core.mixin.CapabilityProviderAccessor;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Forge 1.21.x dropped capability NBT persistence for ItemStacks during the data
 * component migration: {@link ItemStack#save} only encodes data components, and the
 * capability data produced by {@code IItem.getCapabilityProvider} is silently lost
 * on world save/reload. This mixin restores the pre-1.21 behaviour by piggybacking
 * the capability NBT (including gun ammo, magazines and attachments) onto the
 * serialized tag.
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

  private static final String craftingdead$CAPS_KEY = "cdCaps";

  @Inject(method = "save(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;",
      at = @At("RETURN"), cancellable = true)
  private void craftingdead$saveCaps(HolderLookup.Provider provider, Tag tag,
      CallbackInfoReturnable<Tag> cir) {
    CompoundTag caps = ((CapabilityProviderAccessor) (Object) (ItemStack) (Object) this)
        .craftingdead$serializeCaps(provider);
    if (caps != null && !caps.isEmpty() && cir.getReturnValue() instanceof CompoundTag compound) {
      compound.put(craftingdead$CAPS_KEY, caps);
    }
  }

  @Inject(method = "parse(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/Tag;)Ljava/util/Optional;",
      at = @At("RETURN"), cancellable = true)
  private static void craftingdead$parseCaps(HolderLookup.Provider provider, Tag tag,
      CallbackInfoReturnable<java.util.Optional<ItemStack>> cir) {
    if (!(tag instanceof CompoundTag compound)
        || !compound.contains(craftingdead$CAPS_KEY, Tag.TAG_COMPOUND)
        || cir.getReturnValue().isEmpty()) {
      return;
    }
    ItemStack stack = cir.getReturnValue().get();
    ((CapabilityProviderAccessor) (Object) stack)
        .craftingdead$deserializeCaps(provider, compound.getCompound(craftingdead$CAPS_KEY));
    cir.setReturnValue(java.util.Optional.of(stack));
  }
}
