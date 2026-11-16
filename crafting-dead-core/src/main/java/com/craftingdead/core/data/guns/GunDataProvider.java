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

package com.craftingdead.core.data.guns;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import com.craftingdead.core.world.item.gun.GunConfiguration;
import com.craftingdead.core.world.item.gun.GunConfigurations;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

public class GunDataProvider implements DataProvider {

  private static final Logger logger = LogUtils.getLogger();

  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private final PackOutput packOutput;
  private final CompletableFuture<HolderLookup.Provider> registries;

  public GunDataProvider(PackOutput packOutput,
      CompletableFuture<HolderLookup.Provider> registries) {
    this.packOutput = packOutput;
    this.registries = registries;
  }

  @Override
  public CompletableFuture<?> run(CachedOutput cache) {
    return this.registries.thenCompose(provider -> {
      RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, provider);
      CompletableFuture<?>[] futures =
          GunConfigurations.registry.get().getValues().stream()
              .map(gunType -> this.encodeGun(gunType, GunConfigurations.REGISTRY_KEY.location(),
                  ops, cache))
              .toArray(CompletableFuture[]::new);
      return CompletableFuture.allOf(futures);
    });
  }

  private CompletableFuture<?> encodeGun(GunConfiguration gun,
      ResourceLocation registryLocation, RegistryOps<JsonElement> ops, CachedOutput cache) {
    var outputFolder = this.packOutput.getOutputFolder();
    var gunId = Objects.requireNonNull(gun.getRegistryName());
    final String pathString = String.join("/", PackType.SERVER_DATA.getDirectory(),
        gunId.getNamespace(), gunId.getNamespace(), registryLocation.getPath(),
        gunId.getPath() + ".json");
    final var path = outputFolder.resolve(pathString);
    var json = GunConfiguration.DIRECT_CODEC.encodeStart(ops, gun)
        .resultOrPartial(msg -> logger.error("Failed to encode {}: {}", path, msg));
    return json.map(element -> DataProvider.saveStable(cache, element, path))
        .orElseGet(() -> CompletableFuture.completedFuture(null));
  }

  @Override
  public String getName() {
    return "Crafting Dead Guns";
  }
}
