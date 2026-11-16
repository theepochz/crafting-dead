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

package com.craftingdead.core.world.damagesource;

import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.world.entity.grenade.Grenade;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class ModDamageSource {

  public static final ResourceKey<DamageType> BULLET_HEADSHOT_DAMAGE_TYPE =
      register("bullet_headshot");
  public static final ResourceKey<DamageType> BULLET_BODY_DAMAGE_TYPE = register("bullet");
  public static final ResourceKey<DamageType> BLEEDING = register("bleeding");
  public static final ResourceKey<DamageType> GRENADE = register("grenade");
  public static final ResourceKey<DamageType> UNSCALED_EXPLOSION_PLAYER =
      register("unscaled_explosion_player");
  public static final ResourceKey<DamageType> UNSCALED_EXPLOSION =
      register("unscaled_explosion");

  private static ResourceKey<DamageType> register(String name) {
    return ResourceKey.create(Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath("craftingdead", name));
  }

  private static Holder<DamageType> getHolder(Entity entity, ResourceKey<DamageType> key) {
    return entity.level().registryAccess()
        .registryOrThrow(Registries.DAMAGE_TYPE)
        .getHolderOrThrow(key);
  }

  public static DamageSource gun(LivingEntity source, boolean headshot) {
    var key = headshot ? BULLET_HEADSHOT_DAMAGE_TYPE : BULLET_BODY_DAMAGE_TYPE;
    return new DamageSource(getHolder(source, key), source);
  }

  public static DamageSource grenade(Grenade grenade, @Nullable Entity thrower) {
    return new DamageSource(getHolder(grenade, GRENADE), grenade, thrower);
  }

  public static DamageSource bleeding() {
    return new DamageSource(Holder.direct(new DamageType("bleeding",
        net.minecraft.world.damagesource.DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0F)))
            ;
  }

  /**
   * Creates an explosion damage source without difficulty scaling.
   */
  public static DamageSource causeUnscaledExplosionDamage(@Nullable LivingEntity source) {
    var type = new DamageType("explosion",
        net.minecraft.world.damagesource.DamageScaling.NEVER, 0.0F);
    return new DamageSource(Holder.direct(type), source);
  }

  /**
   * Hurt an entity without doing knockback.
   *
   * @param victim - the victim
   * @param source - the source
   * @param amount - the amount of dmg
   * @return the result from {@link Entity#hurt(DamageSource, float)}.
   */
  public static boolean hurtWithoutKnockback(Entity victim, DamageSource source, float amount) {
    if (victim instanceof LivingEntity livingHit) {
      var livingResistance = livingHit.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
      var previousResistance = livingResistance.getBaseValue();

      livingResistance.setBaseValue(Integer.MAX_VALUE);
      var attackResult = livingHit.hurt(source, amount);

      livingResistance.setBaseValue(previousResistance);
      return attackResult;
    }
    return victim.hurt(source, amount);
  }
}
