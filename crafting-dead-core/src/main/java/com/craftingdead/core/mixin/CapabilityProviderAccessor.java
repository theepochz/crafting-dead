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
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.CapabilityProvider;

/**
 * Exposes the protected capability serialization helpers so {@link ItemStackMixin}
 * can persist capability data alongside item data components.
 */
@Mixin(CapabilityProvider.class)
public interface CapabilityProviderAccessor {

  @Invoker("serializeCaps")
  CompoundTag craftingdead$serializeCaps(HolderLookup.Provider provider);

  @Invoker("deserializeCaps")
  void craftingdead$deserializeCaps(HolderLookup.Provider provider, CompoundTag tag);
}