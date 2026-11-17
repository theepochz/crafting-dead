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

package com.craftingdead.core.network;

import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.network.message.play.BlockDestroyParticleMessage;
import com.craftingdead.core.network.message.play.BlockDestroyActionMessage;
import com.craftingdead.core.network.message.play.CancelActionMessage;
import com.craftingdead.core.network.message.play.CrouchMessage;
import com.craftingdead.core.network.message.play.DamageHandcuffsMessage;
import com.craftingdead.core.network.message.play.EnableCombatModeMessage;
import com.craftingdead.core.network.message.play.HitMessage;
import com.craftingdead.core.network.message.play.NPCTriggerPressedMessage;
import com.craftingdead.core.network.message.play.OpenCraftingMenuMessage;
import com.craftingdead.core.network.message.play.OpenEquipmentMenuMessage;
import com.craftingdead.core.network.message.play.OpenStorageMessage;
import com.craftingdead.core.network.message.play.ParachuteSyncMessage;
import com.craftingdead.core.network.message.play.PerformActionMessage;
import com.craftingdead.core.network.message.play.RightClickStateMessage;
import com.craftingdead.core.network.message.play.SecondaryActionMessage;
import com.craftingdead.core.network.message.play.SetFireModeMessage;
import com.craftingdead.core.network.message.play.SyncGunContainerSlotMessage;
import com.craftingdead.core.network.message.play.SyncGunEquipmentSlotMessage;
import com.craftingdead.core.network.message.play.SyncLivingMessage;
import com.craftingdead.core.network.message.play.SyncProtectionConfigMessage;
import com.craftingdead.core.network.message.play.TriggerPressedMessage;
import com.craftingdead.core.network.message.play.TraumaPacket;
import com.craftingdead.core.network.message.play.ValidatePendingHitMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

public enum NetworkChannel {

  PLAY(ResourceLocation.fromNamespaceAndPath(CraftingDead.ID, "play")) {
    @Override
    public void registerMessages(SimpleChannel simpleChannel) {
      simpleChannel
          .messageBuilder(SyncLivingMessage.class, 0x00)
          .encoder(SyncLivingMessage::encode)
          .decoder(SyncLivingMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(OpenEquipmentMenuMessage.class, 0x01)
          .encoder(OpenEquipmentMenuMessage::encode)
          .decoder(OpenEquipmentMenuMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(SecondaryActionMessage.class, 0x02)
          .encoder(SecondaryActionMessage::encode)
          .decoder(SecondaryActionMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(SetFireModeMessage.class, 0x03)
          .encoder(SetFireModeMessage::encode)
          .decoder(SetFireModeMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(TriggerPressedMessage.class, 0x04)
          .encoder(TriggerPressedMessage::encode)
          .decoder(TriggerPressedMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(NPCTriggerPressedMessage.class, 0x05)
          .encoder(NPCTriggerPressedMessage::encode)
          .decoder(NPCTriggerPressedMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(SyncGunContainerSlotMessage.class, 0x06)
          .encoder(SyncGunContainerSlotMessage::encode)
          .decoder(SyncGunContainerSlotMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(OpenStorageMessage.class, 0x07)
          .encoder(OpenStorageMessage::encode)
          .decoder(OpenStorageMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(PerformActionMessage.class, 0x08)
          .encoder(PerformActionMessage::encode)
          .decoder(PerformActionMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(CancelActionMessage.class, 0x09)
          .encoder(CancelActionMessage::encode)
          .decoder(CancelActionMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(ValidatePendingHitMessage.class, 0x0A)
          .encoder(ValidatePendingHitMessage::encode)
          .decoder(ValidatePendingHitMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(CrouchMessage.class, 0x0B)
          .encoder(CrouchMessage::encode)
          .decoder(CrouchMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(HitMessage.class, 0x0C)
          .encoder(HitMessage::encode)
          .decoder(HitMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(SyncGunEquipmentSlotMessage.class, 0x0D)
          .encoder(SyncGunEquipmentSlotMessage::encode)
          .decoder(SyncGunEquipmentSlotMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(EnableCombatModeMessage.class, 0x0E)
          .encoder(EnableCombatModeMessage::encode)
          .decoder(EnableCombatModeMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(DamageHandcuffsMessage.class, 0x0F)
          .encoder(DamageHandcuffsMessage::encode)
          .decoder(DamageHandcuffsMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(ParachuteSyncMessage.class, 0x11)
          .encoder(ParachuteSyncMessage::encode)
          .decoder(ParachuteSyncMessage::decode)
          .consumer((ParachuteSyncMessage msg, net.minecraftforge.event.network.CustomPayloadEvent.Context ctx) -> ctx.setPacketHandled(ParachuteSyncMessage.handle(msg, ctx)))
          .add();

      simpleChannel
          .messageBuilder(OpenCraftingMenuMessage.class, 0x12)
          .encoder(OpenCraftingMenuMessage::encode)
          .decoder(OpenCraftingMenuMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(TraumaPacket.class, 0x13)
          .encoder(TraumaPacket::encode)
          .decoder(TraumaPacket::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(SyncProtectionConfigMessage.class, 0x14)
          .encoder(SyncProtectionConfigMessage::encode)
          .decoder(SyncProtectionConfigMessage::decode)
          .consumer((msg, ctx) -> ctx.setPacketHandled(msg.handle(ctx)))
          .add();

      simpleChannel
          .messageBuilder(RightClickStateMessage.class, 0x15)
          .encoder(RightClickStateMessage::encode)
          .decoder(RightClickStateMessage::decode)
          .consumer((RightClickStateMessage msg, net.minecraftforge.event.network.CustomPayloadEvent.Context ctx) -> ctx.setPacketHandled(RightClickStateMessage.handle(msg, ctx)))
          .add();

      simpleChannel
          .messageBuilder(BlockDestroyParticleMessage.class, 0x16)
          .encoder(BlockDestroyParticleMessage::encode)
          .decoder(BlockDestroyParticleMessage::decode)
          .consumer((BlockDestroyParticleMessage msg, net.minecraftforge.event.network.CustomPayloadEvent.Context ctx) -> ctx.setPacketHandled(BlockDestroyParticleMessage.handle(msg, ctx)))
          .add();

      simpleChannel
          .messageBuilder(BlockDestroyActionMessage.class, 0x17)
          .encoder(BlockDestroyActionMessage::encode)
          .decoder(BlockDestroyActionMessage::decode)
          .consumer((BlockDestroyActionMessage msg, net.minecraftforge.event.network.CustomPayloadEvent.Context ctx) -> ctx.setPacketHandled(BlockDestroyActionMessage.handle(msg, ctx)))
          .add();
    }
  };

  /**
   * Network protocol version.
   */
    private static final int NETWORK_VERSION = 1;
  /**
   * Prevents re-registering messages.
   */
  private static boolean loaded;
  /**
   * Simple channel.
   */
  private final SimpleChannel simpleChannel;

  NetworkChannel(ResourceLocation channelName) {
    this.simpleChannel = ChannelBuilder
        .named(channelName)
        .networkProtocolVersion(NETWORK_VERSION)
        .clientAcceptedVersions(Channel.VersionTest.exact(NETWORK_VERSION))
        .serverAcceptedVersions(Channel.VersionTest.exact(NETWORK_VERSION))
        .simpleChannel();
  }

  protected abstract void registerMessages(SimpleChannel simpleChannel);

  public SimpleChannel getSimpleChannel() {
    return this.simpleChannel;
  }

  public static void loadChannels() {
    if (!loaded) {
      for (NetworkChannel channel : NetworkChannel.values()) {
        channel.registerMessages(channel.simpleChannel);
        channel.simpleChannel.build();
      }
      loaded = true;
    }
  }
}
