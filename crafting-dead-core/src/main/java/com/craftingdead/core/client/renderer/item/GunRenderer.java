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

package com.craftingdead.core.client.renderer.item;

import com.mojang.math.Axis;

import net.minecraft.world.item.ItemDisplayContext;


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.capability.CapabilityUtil;
import com.craftingdead.core.client.model.geom.ModModelLayers;
import com.craftingdead.core.client.util.RenderUtil;
import com.craftingdead.core.util.EasingFunction;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.inventory.GunCraftSlotType;
import com.craftingdead.core.world.item.GunItem;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.attachment.Attachment;
import com.craftingdead.core.world.item.gun.attachment.Attachments;
import com.craftingdead.core.world.item.gun.skin.Paint;
import com.craftingdead.core.world.item.scope.Scope;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Transformation;
import org.joml.Vector3f;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.SimpleModelState;

public class GunRenderer implements CombatSlotItemRenderer {

  private static final Pair<Transformation, Transformation> IDENTITY_HAND_TRANSFORMS =
      Pair.of(Transformation.identity(), Transformation.identity());

  private static final String GUN_TEXTURE_REFERENCE = "gun";

  private static final int FLASH_TEXTURE_CHANGE_TIMEOUT_MS = 250;

  private static final int SPRINT_TRANSITION_MS = 300;

  private static final int AIMING_TRANSITION_MS = 200;

  private static final net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();

  private final Minecraft minecraft = Minecraft.getInstance();

  private final Map<Integer, BakedModel> cachedModels = new Int2ObjectOpenHashMap<>();

  private final GunItem item;

  private final GunRendererProperties properties;

  private ModelPart muzzleFlashModel;

  private long lastFlashTime = 0;

  private long sprintStartTimeMs;
  private boolean wasSprinting;

  private long aimingStartTimeMs;
  private boolean wasAiming;

  public GunRenderer(GunItem item, GunRendererProperties properties) {
    this.item = item;
    this.properties = properties;
  }

  @Override
  public void renderInCombatSlot(ItemStack itemStack, PoseStack poseStack, float partialTick,
      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
    var gun = CapabilityUtil.getOrThrow(Gun.CAPABILITY, itemStack, Gun.class);
    this.renderGun(gun, false, true, itemStack.hasFoil(), 0.0F,
        ItemDisplayContext.GUI, partialTick, poseStack, bufferSource, packedLight,
        packedOverlay);
  }

  @Override
  public void rotateCamera(ItemStack itemStack, LivingEntity livingEntity, float partialTick,
      Vector3f rotations) {
    var gun = CapabilityUtil.getOrThrow(Gun.CAPABILITY, itemStack, Gun.class);
    gun.getClient().getAnimationController().applyCamera(partialTick, rotations);
  }

  @Override
  public boolean handlePerspective(ItemStack item, ItemDisplayContext transformType) {
    return switch (transformType) {
      case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND,
          FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND,
          FIXED, HEAD -> true;
      default -> false;
    };
  }

  @Override
  public void render(
      ItemStack itemStack,
      ItemDisplayContext transformType,
      @Nullable LivingExtension<?, ?> living,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight,
      int packedOverlay) {

    var scope = itemStack.getCapability(Scope.CAPABILITY).orElse(null);
    var scoping = scope != null && living != null && scope.isScoping(living);
    if (scoping && scope.getOverlayTexture(living).isPresent()) {
      return;
    }

    var partialTick = this.minecraft.isPaused() ? 1.0F
        : this.minecraft.getTimer().getGameTimeDeltaTicks();

    var gun = CapabilityUtil.getOrThrow(Gun.CAPABILITY, itemStack, Gun.class);

    poseStack.pushPose();
    {
      switch (transformType) {
        case FIRST_PERSON_LEFT_HAND:
        case FIRST_PERSON_RIGHT_HAND:
          if (living != null && living.entity() instanceof AbstractClientPlayer player) {
            this.renderFirstPerson(player, itemStack, gun, scoping, transformType,
                partialTick, poseStack, bufferSource, packedLight, packedOverlay);
          }
          break;
        case THIRD_PERSON_LEFT_HAND:
        case THIRD_PERSON_RIGHT_HAND:
          gun.getClient().getAnimationController().apply(partialTick, poseStack);
          this.renderGun(gun, true, false, itemStack.hasFoil(), 0.0F,
              transformType, partialTick, poseStack, bufferSource, packedLight,
              packedOverlay);
          break;
        case HEAD:
          poseStack.pushPose();
          poseStack.mulPose(this.properties.backTransform().getLeftRotation());
      poseStack.translate(this.properties.backTransform().getTranslation().x(),
          this.properties.backTransform().getTranslation().y(),
          this.properties.backTransform().getTranslation().z());
      poseStack.scale(this.properties.backTransform().getScale().x(),
          this.properties.backTransform().getScale().y(),
          this.properties.backTransform().getScale().z());
      poseStack.mulPose(this.properties.backTransform().getRightRotation());
          this.renderGun(gun, true, false, itemStack.hasFoil(), 0.0F,
              transformType, partialTick, poseStack, bufferSource, packedLight,
              packedOverlay);
          poseStack.popPose();
          break;
        case FIXED:
          poseStack.scale(1.5F, 1.5F, 1.5F);
          poseStack.translate(-0.25F, 0, 0);
          poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
          poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        default:
          this.renderGun(gun, true, false, itemStack.hasFoil(), 0.0F,
              transformType, partialTick, poseStack, bufferSource, packedLight,
              packedOverlay);
          break;
      }
    }
    poseStack.popPose();
  }

  private void renderFirstPersonArms(
      AbstractClientPlayer playerEntity,
      ItemStack itemStack,
      Gun gun,
      float aimingPct,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight) {

    final var playerRenderer =
        (PlayerRenderer) this.minecraft.getEntityRenderDispatcher().getRenderer(playerEntity);

    var mainArm = playerEntity.getMainArm();

    var heldTransforms = this.properties.handTransforms().getOrDefault(
        mainArm == HumanoidArm.RIGHT
            ? GunRendererProperties.HandTransform.HELD_RIGHT_HANDED
            : GunRendererProperties.HandTransform.HELD_LEFT_HANDED,
        IDENTITY_HAND_TRANSFORMS);

    Transformation rightHandTransforms;
    Transformation leftHandTransforms;
    if (aimingPct > 0) {
      var aimingTransforms = this.properties.handTransforms().getOrDefault(
          mainArm == HumanoidArm.RIGHT
              ? GunRendererProperties.HandTransform.AIMING_RIGHT_HANDED
              : GunRendererProperties.HandTransform.AIMING_LEFT_HANDED,
          IDENTITY_HAND_TRANSFORMS);
      rightHandTransforms = heldTransforms.getFirst().slerp(aimingTransforms.getFirst(),
          aimingPct);
      leftHandTransforms = heldTransforms.getSecond().slerp(aimingTransforms.getSecond(),
          aimingPct);
    } else {
      rightHandTransforms = heldTransforms.getFirst();
      leftHandTransforms = heldTransforms.getSecond();
    }

    // Right Arm
    poseStack.pushPose();
    {
      gun.getClient().getAnimationController().applyArm(
          playerEntity.getMainArm() == HumanoidArm.RIGHT
              ? InteractionHand.MAIN_HAND
              : InteractionHand.OFF_HAND,
          HumanoidArm.RIGHT,
          partialTicks, poseStack);
      poseStack.translate(0.75F, -0.75F, -0.6F);

      // Push + apply (matches 1.18 rightHandTransforms.push(poseStack) semantics)
      poseStack.pushPose();
      poseStack.mulPose(rightHandTransforms.getLeftRotation());
      poseStack.translate(rightHandTransforms.getTranslation().x(), rightHandTransforms.getTranslation().y(), rightHandTransforms.getTranslation().z());
      poseStack.scale(rightHandTransforms.getScale().x(), rightHandTransforms.getScale().y(), rightHandTransforms.getScale().z());
      poseStack.mulPose(rightHandTransforms.getRightRotation());
      {
        poseStack.mulPose(Axis.YP.rotationDegrees(95.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(100.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));

        poseStack.translate(0.9F, -0.75F, 0.5F);

        playerRenderer.renderRightHand(poseStack, bufferSource, packedLight, playerEntity);
      }
      poseStack.popPose();
    }
    poseStack.popPose();

    // Left Arm
    poseStack.pushPose();
    {
      gun.getClient().getAnimationController().applyArm(
          playerEntity.getMainArm() == HumanoidArm.LEFT ? InteractionHand.MAIN_HAND
              : InteractionHand.OFF_HAND,
          HumanoidArm.LEFT,
          partialTicks, poseStack);

      poseStack.translate(0.75F, -0.75F, -0.75F);

      // Push + apply (matches 1.18 leftHandTransforms.push(poseStack) semantics)
      poseStack.pushPose();
      poseStack.mulPose(leftHandTransforms.getLeftRotation());
      poseStack.translate(leftHandTransforms.getTranslation().x(), leftHandTransforms.getTranslation().y(), leftHandTransforms.getTranslation().z());
      poseStack.scale(leftHandTransforms.getScale().x(), leftHandTransforms.getScale().y(), leftHandTransforms.getScale().z());
      poseStack.mulPose(leftHandTransforms.getRightRotation());
      {
        poseStack.mulPose(Axis.YP.rotationDegrees(95.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(95.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(120.0F));

        poseStack.translate(0.05F, -1.05F, 0.30F);

        playerRenderer.renderLeftHand(poseStack, bufferSource, packedLight, playerEntity);
      }
      poseStack.popPose();
    }
    poseStack.popPose();
  }

  private void renderFlash(Gun gun, PoseStack matrixStack, MultiBufferSource bufferSource,
      int packedOverlay) {
    matrixStack.pushPose();
    {
      var currentTime = Util.getMillis();
      var deltaTime = currentTime - this.lastFlashTime;

      int texture;
      if (deltaTime > FLASH_TEXTURE_CHANGE_TIMEOUT_MS) {
        this.lastFlashTime = currentTime;
        deltaTime = FLASH_TEXTURE_CHANGE_TIMEOUT_MS;
      }

      // Every n milliseconds, change the muzzle flash texture
      texture = (int) ((deltaTime / 100) + 1);
      var randomScale = (random.nextInt(8) + 3) / 10.0F;

      var transform = this.properties.muzzleFlashTransform();

      var scale = transform.getScale();
      matrixStack.scale(
          scale.x() + randomScale,
          scale.y() + randomScale,
          scale.z() + randomScale);

      var x = transform.getTranslation().x();
      var y = transform.getTranslation().y();
      var z = transform.getTranslation().z();
      if (gun.getAttachments().containsValue(Attachments.SUPPRESSOR.get())) {
        z -= 0.4;
      }

      matrixStack.translate(
          x - randomScale * x / 2,
          y - randomScale * y / 2,
          z - randomScale * z / 2);

      var flashBuffer = bufferSource.getBuffer(RenderType.beaconBeam(
          ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "textures/flash/flash" + texture + ".png"),
          true));
      matrixStack.scale(randomScale + 0.5F, randomScale + 0.5F, randomScale + 0.5F);
      this.muzzleFlashModel.render(matrixStack, flashBuffer, RenderUtil.FULL_LIGHT,
          packedOverlay);
    }
    matrixStack.popPose();
  }

  private void renderFirstPerson(
      AbstractClientPlayer playerEntity,
      ItemStack itemStack,
      Gun gun,
      boolean aiming,
      ItemDisplayContext transformType,
      float partialTicks,
      PoseStack poseStack,
      MultiBufferSource renderTypeBuffer,
      int packedLight,
      int packedOverlay) {

    final var flash = gun.getClient().isFlashing();
    if (flash) {
      packedLight = RenderUtil.FULL_LIGHT;
    }

    // ============== Animations ==============

    if (this.wasSprinting != playerEntity.isSprinting()) {
      this.sprintStartTimeMs = Util.getMillis();
      this.wasSprinting = playerEntity.isSprinting();
    }

    var sprintingPct = Mth.clamp(
        (float) (Util.getMillis() - this.sprintStartTimeMs) / SPRINT_TRANSITION_MS,
        0.0F, 1.0F);
    if (!playerEntity.isSprinting()) {
      sprintingPct = 1.0F - sprintingPct;
    }

    sprintingPct = EasingFunction.SINE_IN_OUT.apply(sprintingPct);

    if (this.wasAiming != aiming) {
      this.aimingStartTimeMs = Util.getMillis();
      this.wasAiming = aiming;
    }

    var aimingPct = Mth.clamp(
        (float) (Util.getMillis() - this.aimingStartTimeMs) / AIMING_TRANSITION_MS,
        0.0F, 1.0F);
    if (!aiming) {
      aimingPct = 1.0F - aimingPct;
    }

    aimingPct = EasingFunction.SINE_IN_OUT.apply(aimingPct);

    // ============== Animations End ==============

    if (sprintingPct > 0) {
      var sprinting = Transformation.identity()
          .slerp(this.properties.sprintingTransform(), sprintingPct);
      // Push first so the unconditional popPose below stays balanced (matches 1.18
      // TransformationHelper.slerp(...).push(poseStack) semantics).
      poseStack.pushPose();
      poseStack.mulPose(sprinting.getLeftRotation());
      poseStack.translate(sprinting.getTranslation().x(), sprinting.getTranslation().y(),
          sprinting.getTranslation().z());
      poseStack.scale(sprinting.getScale().x(), sprinting.getScale().y(),
          sprinting.getScale().z());
      poseStack.mulPose(sprinting.getRightRotation());
    } else {
      poseStack.pushPose();
    }
    {
      if (!aiming && flash) {
        this.renderFlash(gun, poseStack, renderTypeBuffer, packedOverlay);
      }

      this.renderFirstPersonArms(playerEntity, itemStack, gun, aimingPct, partialTicks,
          poseStack, renderTypeBuffer, packedLight);

      gun.getClient().getAnimationController().apply(partialTicks, poseStack);

      this.renderGun(gun, true, false, itemStack.hasFoil(), aimingPct, transformType,
          partialTicks, poseStack, renderTypeBuffer, packedLight, packedOverlay);
    }
    poseStack.popPose();
  }

  private void renderGun(
      Gun gun,
      boolean renderAttachments,
      boolean renderDefaultMagazine,
      boolean foil,
      float aimingPct,
      ItemDisplayContext transformType,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int packedLight,
      int packedOverlay) {

    var skinTextureLocation = gun.getSkin() == null
        ? null
        : gun.getSkin().getTextureLocation(gun.getItemStack().getItem().builtInRegistryHolder().key().location());

    final var color = gun.getPaintStack().getCapability(Paint.CAPABILITY)
        .resolve()
        .map(paint -> paint.getColor().orElse(0xFFFFFFFF))
        .orElse(0xFFFFFFFF);

    Map<String, Either<Material, String>> textures = null;
    if (skinTextureLocation != null) {
      textures = Map.of(GUN_TEXTURE_REFERENCE,
          Either.left(new Material(InventoryMenu.BLOCK_ATLAS, skinTextureLocation)));
    }
    var bakedModel = this.getBakedModel(this.properties.modelLocation(), textures);

    var tempPoseStack = new PoseStack();
    // 1.21: handlePerspective removed; applyTransform returns void and mutates the stack
    bakedModel.applyTransform(transformType, tempPoseStack, false);
    var normalTransform = new Transformation(tempPoseStack.last().pose());

    var overbarrel = gun.getAttachments().get(GunCraftSlotType.OVERBARREL_ATTACHMENT);
    ResourceLocation scopeAimKey = overbarrel == null ? null
        : net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(overbarrel.asItem());
    var aimTransform = gun.hasIronSight() || scopeAimKey == null
        ? this.properties.aimTransform()
        : this.properties.scopeAimTransform().getOrDefault(scopeAimKey,
            this.properties.aimTransform());
    var perspectiveTransform = aimingPct > 0
        ? normalTransform.slerp(aimTransform, aimingPct)
        : normalTransform;

    // Push + apply (matches 1.18 perspectiveTransform.push(poseStack) semantics)
    poseStack.pushPose();
    poseStack.mulPose(perspectiveTransform.getLeftRotation());
      poseStack.translate(perspectiveTransform.getTranslation().x(),
          perspectiveTransform.getTranslation().y(), perspectiveTransform.getTranslation().z());
      poseStack.scale(perspectiveTransform.getScale().x(), perspectiveTransform.getScale().y(),
          perspectiveTransform.getScale().z());
      poseStack.mulPose(perspectiveTransform.getRightRotation());
    {
      this.renderBakedModel(bakedModel, foil, color, transformType,
          poseStack, bufferSource, packedLight, packedOverlay);

      var magazineStack = renderDefaultMagazine
          ? gun.getDefaultMagazineStack()
          : gun.getAmmoProvider().getMagazineStack();
      if (!magazineStack.isEmpty()) {
        this.renderMagazine(magazineStack, skinTextureLocation, color, foil, transformType,
            poseStack, bufferSource, packedLight, packedOverlay);
      }

      if (renderAttachments) {
        this.renderAttachments(gun, skinTextureLocation, color, foil, transformType,
            partialTick, poseStack, bufferSource, packedLight, packedOverlay);
      }
    }
    poseStack.popPose();
  }

  protected final void renderBakedModel(BakedModel bakedModel, boolean foil, int colour,
      ItemDisplayContext transformType, PoseStack poseStack,
      MultiBufferSource renderTypeBuffer, int packedLight, int packedOverlay) {
    this.renderBakedModel(bakedModel, foil, colour, transformType, poseStack, renderTypeBuffer,
        packedLight, packedOverlay, null);
  }

  /**
   * @param spriteOverride when non-null, every quad is re-bound to this sprite. Child models
   *        (magazines, iron sights) only reference {@code #gun} and declare no textures of
   *        their own - that reference used to be resolved at bake time by Forge, which no
   *        longer exists in 1.21. Without this the quads fall back to {@code missingno}
   *        (black/pink checkerboard).
   */
  protected final void renderBakedModel(BakedModel bakedModel, boolean foil, int colour,
      ItemDisplayContext transformType, PoseStack poseStack,
      MultiBufferSource renderTypeBuffer, int packedLight, int packedOverlay,
      @Nullable TextureAtlasSprite spriteOverride) {
    poseStack.pushPose();
    {
      // Models rendered through this path are either the gun itself (whose item
      // transform was already applied by the caller) or children of the gun (magazine,
      // iron sights, attachments) which are authored in the gun's model space.
      // Applying an item transform again would push them out of the gun and leave them
      // unlit/appearing black, so it is deliberately skipped here.
      poseStack.translate(-0.5D, -0.5D, -0.5D);
      VertexConsumer vertexBuilder = ItemRenderer.getFoilBuffer(renderTypeBuffer,
          Sheets.translucentCullBlockSheet(), true, foil);
      var bakedQuads = bakedModel.getQuads(null, null, random);
      var pose = poseStack.last();
      for (var bakedQuad : bakedQuads) {
        if (spriteOverride != null) {
          bakedQuad = remapQuadSprite(bakedQuad, spriteOverride);
        }
        var red = (colour >> 16 & 255) / 255.0F;
        var green = (colour >> 8 & 255) / 255.0F;
        var blue = (colour & 255) / 255.0F;
        vertexBuilder.putBulkData(pose, bakedQuad, red, green, blue, 1.0F,
            packedLight, packedOverlay);
      }
    }
    poseStack.popPose();
  }

  /**
   * Resolves the sprite that child models should use: the gun's skin texture when one is
   * applied, otherwise the gun's own texture.
   */
  private TextureAtlasSprite getGunSprite(@Nullable ResourceLocation skinTextureLocation) {
    return skinTextureLocation == null
        ? this.getGunRenderMaterial().sprite()
        : new Material(InventoryMenu.BLOCK_ATLAS, skinTextureLocation).sprite();
  }

  /**
   * Re-binds a baked quad to a different sprite. The vertex data holds UVs in normalized
   * atlas coordinates, so they are inverse-mapped through the old sprite (usually
   * {@code missingno}) back to 0..1 face space and then re-projected onto the target
   * sprite.
   */
  private static net.minecraft.client.renderer.block.model.BakedQuad remapQuadSprite(
      net.minecraft.client.renderer.block.model.BakedQuad quad,
      TextureAtlasSprite target) {
    var old = quad.getSprite();
    if (old == target || old.getU1() - old.getU0() == 0.0F
        || old.getV1() - old.getV0() == 0.0F) {
      return quad;
    }
    var vertices = quad.getVertices().clone();
    final int vertexIntSize = 8;
    final int uvIndex = 4;
    for (int v = 0; v < 4; v++) {
      int base = v * vertexIntSize + uvIndex;
      float u = Float.intBitsToFloat(vertices[base]);
      float vCoord = Float.intBitsToFloat(vertices[base + 1]);
      // Back to 0..1 face space through the old sprite's atlas footprint.
      float localU = (u - old.getU0()) / (old.getU1() - old.getU0());
      float localV = (vCoord - old.getV0()) / (old.getV1() - old.getV0());
      vertices[base] = Float.floatToRawIntBits(target.getU(localU));
      vertices[base + 1] = Float.floatToRawIntBits(target.getV(localV));
    }
    return new net.minecraft.client.renderer.block.model.BakedQuad(
        vertices, quad.getTintIndex(), quad.getDirection(), target,
        quad.isShade(), quad.hasAmbientOcclusion());
  }

  private BakedModel getBakedModel(ResourceLocation modelLocation,
      @Nullable Map<String, Either<Material, String>> textures) {
    return this.cachedModels.computeIfAbsent(
        modelLocation.hashCode() + (textures == null ? 0 : textures.hashCode()), key -> {
          // Custom textures overrides are no longer supported at bake-time in 1.21;
          // fall back to the baked model from ModelManager (top-level registered models
          // are included in the baked registry).
          return this.minecraft.getModelManager().getModel(toModelLocation(modelLocation));
        });
  }

  private void renderAttachments(
      Gun gun,
      @Nullable ResourceLocation skinTextureLocation,
      int colour,
      boolean foil,
      ItemDisplayContext transformType,
      float partialTicks,
      PoseStack matrixStack,
      MultiBufferSource renderTypeBuffer,
      int packedLight,
      int packedOverlay) {

    if (gun.hasIronSight()) {
      for (var ironSight : this.properties.ironSights()) {
        poseStack0Push(matrixStack, ironSight.getSecond());
        {
          var bakedModel = this.getBakedModel(ironSight.getFirst(), null);
          this.renderBakedModel(bakedModel, foil, colour, ItemDisplayContext.NONE, matrixStack,
              renderTypeBuffer, packedLight, packedOverlay, this.getGunSprite(skinTextureLocation));
        }
        matrixStack.popPose();
      }
    }

    for (var attachment : gun.getAttachments().values()) {
      var transform = this.properties.attachmentTransforms()
          .getOrDefault(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(attachment.asItem()),
          Transformation.identity());
      poseStack0Push(matrixStack, transform);
      {
        var bakedModel =
            this.getBakedModel(
            getAttachmentModelLocation(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(attachment.asItem())),
            null);
        this.renderBakedModel(bakedModel, foil, colour, ItemDisplayContext.NONE, matrixStack,
            renderTypeBuffer, packedLight, packedOverlay);
      }
      matrixStack.popPose();
    }
  }

  private void renderMagazine(
      ItemStack magazineStack,
      @Nullable ResourceLocation skinTextureLocation,
      int color,
      boolean foil,
      ItemDisplayContext transformType,
      PoseStack poseStack,
      MultiBufferSource renderTypeBuffer,
      int packedLight,
      int packedOverlay) {
    var transform = this.properties.magazineTransforms()
        .getOrDefault(magazineStack.getItem().builtInRegistryHolder().key().location(), Transformation.identity());
    poseStack0Push(poseStack, transform);
    {
      var modelLocation = getMagazineModelLocation(magazineStack.getItem().builtInRegistryHolder().key().location());

      var magazineBakedModel = this.getBakedModel(modelLocation, null);

      // The magazine model is positioned in the gun's model space, which is already
      // transformed by the gun's item transform. Passing ItemDisplayContext.NONE keeps
      // the baked model's own transform out of the way.
      this.renderBakedModel(magazineBakedModel, foil, color, ItemDisplayContext.NONE, poseStack,
          renderTypeBuffer, packedLight, packedOverlay, this.getGunSprite(skinTextureLocation));
    }
    poseStack.popPose();
  }

  private static net.minecraft.client.resources.model.ModelResourceLocation toModelLocation(
      ResourceLocation location) {
    // 1.21: ModelResourceLocation is a record, not a ResourceLocation subclass
    return net.minecraft.client.resources.model.ModelResourceLocation.inventory(location);
  }

  private static void poseStack0Push(PoseStack poseStack, Transformation transform) {
    // Push + apply (matches 1.18 transform.push(poseStack) semantics)
    poseStack.pushPose();
    poseStack.mulPose(transform.getLeftRotation());
    poseStack.translate(transform.getTranslation().x(), transform.getTranslation().y(),
        transform.getTranslation().z());
    poseStack.scale(transform.getScale().x(), transform.getScale().y(), transform.getScale().z());
    poseStack.mulPose(transform.getRightRotation());
  }

  private Material getGunRenderMaterial() {
    // 1.21: ForgeModelBakery removed; use the baked model's particle icon.
    var bakedModel = this.minecraft.getModelManager()
        .getModel(toModelLocation(this.properties.modelLocation()));
    var particleSprite = bakedModel.getParticleIcon();
    return new Material(InventoryMenu.BLOCK_ATLAS, particleSprite.contents().name());
  }

  @Override
  public Collection<ResourceLocation> getModelDependencies() {
    final Set<ResourceLocation> dependencies = new HashSet<>();
    dependencies.addAll(this.properties.ironSights().stream()
        .map(Pair::getFirst)
        .collect(Collectors.toSet()));
    dependencies.addAll(this.item.getAcceptedAttachments().stream()
        .map(a -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(a.asItem()))
        .map(GunRenderer::getAttachmentModelLocation)
        .collect(Collectors.toSet()));
    dependencies.addAll(this.item.getAcceptedMagazines().stream()
        .map(it -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it))
        .map(GunRenderer::getMagazineModelLocation)
        .collect(Collectors.toSet()));
    dependencies.add(this.properties.modelLocation());
    return dependencies;
  }

  @Override
  public Collection<Material> getMaterials() {
    return Collections.emptySet();
  }

  @Override
  public void refreshCachedModels(Function<ModelLayerLocation, ModelPart> modelBaker) {
    this.cachedModels.clear();
    this.muzzleFlashModel = modelBaker.apply(ModModelLayers.MUZZLE_FLASH);
  }

  @Override
  public ItemRendererType<?, ?> getType() {
    return ItemRendererTypes.GUN;
  }

  private static ResourceLocation getAttachmentModelLocation(ResourceLocation attachmentName) {
    return ResourceLocation.fromNamespaceAndPath(attachmentName.getNamespace(), "attachment/" + attachmentName.getPath());
  }

  private static ResourceLocation getMagazineModelLocation(ResourceLocation magazineName) {
    return ResourceLocation.fromNamespaceAndPath(magazineName.getNamespace(), "magazine/" + magazineName.getPath());
  }

  public static LayerDefinition createMuzzleFlashBodyLayer() {
    var mesh = new MeshDefinition();
    var root = mesh.getRoot();
    root.addOrReplaceChild("box0",
        CubeListBuilder.create()
            .mirror()
            .texOffs(1, 1)
            .addBox(0F, 0F, 0F, 8, 8, 0)
            .mirror(false),
        PartPose.offset(-4F, -4F, 0F));
    root.addOrReplaceChild("box1",
        CubeListBuilder.create()
            .mirror()
            .texOffs(9, 1)
            .addBox(-4F, 0F, 0F, 8, 0, 15)
            .mirror(false),
        PartPose.offsetAndRotation(0F, 0F, -15F, 0F, 0F, -0.7853982F));
    root.addOrReplaceChild("box2",
        CubeListBuilder.create()
            .mirror()
            .texOffs(1, 17)
            .addBox(-4F, 0F, 0F, 8, 0, 15)
            .mirror(false),
        PartPose.offsetAndRotation(0F, 0F, -15F, 0F, 0F, -2.373648F));
    return LayerDefinition.create(mesh, 64, 64);
  }
}
