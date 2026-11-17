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

package com.craftingdead.core.util;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Replacement for the removed {@code ForgeRegistryEntry} class. Provides a lazily resolved
 * registry name backed by the registry the entry belongs to.
 */
public abstract class RegistryEntry<V> {

  @Nullable
  private ResourceLocation registryName;

  protected abstract IForgeRegistry<V> getRegistry();

  @SuppressWarnings("unchecked")
  public ResourceLocation getRegistryName() {
    if (this.registryName == null) {
      this.registryName = this.getRegistry().getKey((V) this);
    }
    return this.registryName;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || this.getClass() != obj.getClass()) {
      return false;
    }
    var that = (RegistryEntry<V>) obj;
    var thisName = this.getRegistryName();
    var thatName = that.getRegistryName();
    return thisName != null && thisName.equals(thatName);
  }

  @Override
  public int hashCode() {
    // NOTE: must never trigger getRegistryName() - the Forge registry looks items up by
    // their own hashCode, so resolving the name here causes infinite recursion. Registry
    // entries are singletons, so identity hash is correct and stable.
    return System.identityHashCode(this);
  }
}
