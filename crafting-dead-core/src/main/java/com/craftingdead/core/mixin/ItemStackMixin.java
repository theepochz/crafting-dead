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
 * component migration: {@link ItemStack#save} only encodes data components, and
 * {@link ItemStack#copy}/{@link ItemStack#split} create the new stack from the
 * component map alone - capabilities are silently dropped.
 *
 * <p>This mixin restores the pre-1.21 behaviour at both points:
 * <ul>
 * <li>{@code save}/{@code parse} - capability data (gun ammo, magazines, attachments)
 * is piggybacked onto the serialized tag so it survives world saves;</li>
 * <li>{@code copy}/{@code split}/{@code copyAndClear} - capability data is transferred
 * to the new stack so moving an item through container menus, dropping/picking it up
 * or hotbar swaps do not erase it.</li>
 * </ul>
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

  @Inject(method = "copy()Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
  private void craftingdead$copyCaps(CallbackInfoReturnable<ItemStack> cir) {
    ItemStack newStack = cir.getReturnValue();
    if (newStack.isEmpty()) {
      return;
    }
    var provider = craftingdead$lookupProvider();
    if (provider == null) {
      // No registry access available (e.g. main menu) - nothing meaningful to serialize.
      return;
    }
    CompoundTag caps = ((CapabilityProviderAccessor) (Object) (ItemStack) (Object) this)
        .craftingdead$serializeCaps(provider);
    if (caps != null && !caps.isEmpty()) {
      ((CapabilityProviderAccessor) (Object) newStack)
          .craftingdead$deserializeCaps(provider, caps);
    }
  }

  @Inject(method = "split(I)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
  private void craftingdead$splitCaps(int count, CallbackInfoReturnable<ItemStack> cir) {
    ItemStack newStack = cir.getReturnValue();
    if (newStack.isEmpty()) {
      return;
    }
    var provider = craftingdead$lookupProvider();
    if (provider == null) {
      return;
    }
    CompoundTag caps = ((CapabilityProviderAccessor) (Object) (ItemStack) (Object) this)
        .craftingdead$serializeCaps(provider);
    if (caps != null && !caps.isEmpty()) {
      ((CapabilityProviderAccessor) (Object) newStack)
          .craftingdead$deserializeCaps(provider, caps);
    }
  }

  @Inject(method = "copyAndClear()Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
  private void craftingdead$copyAndClearCaps(CallbackInfoReturnable<ItemStack> cir) {
    ItemStack newStack = cir.getReturnValue();
    if (newStack.isEmpty()) {
      return;
    }
    var provider = craftingdead$lookupProvider();
    if (provider == null) {
      return;
    }
    CompoundTag caps = ((CapabilityProviderAccessor) (Object) (ItemStack) (Object) this)
        .craftingdead$serializeCaps(provider);
    if (caps != null && !caps.isEmpty()) {
      ((CapabilityProviderAccessor) (Object) newStack)
          .craftingdead$deserializeCaps(provider, caps);
    }
  }

  /**
   * Resolves a registry lookup provider for capability serialization. Common code
   * cannot reference client classes, so the client side is resolved reflectively.
   */
  private static HolderLookup.Provider craftingdead$lookupProvider() {
    try {
      var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
      if (server != null) {
        return server.registryAccess();
      }
    } catch (Throwable ignored) {
      // Not on the server side.
    }
    try {
      var mcClass = Class.forName("net.minecraft.client.Minecraft");
      var mc = mcClass.getMethod("getInstance").invoke(null);
      var level = mcClass.getField("level").get(mc);
      if (level != null) {
        return ((net.minecraft.world.level.Level) level).registryAccess();
      }
    } catch (Throwable ignored) {
      // No client level loaded.
    }
    return null;
  }
}
