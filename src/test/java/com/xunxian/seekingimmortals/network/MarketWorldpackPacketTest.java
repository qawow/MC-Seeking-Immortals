package com.xunxian.seekingimmortals.network;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketWorldpackPacketTest {
    @Test
    void syncShopDataRoundTrips() {
        SyncShopDataPacket packet = new SyncShopDataPacket(
                "market_herbal_stall",
                "screen.seeking_immortals.shop.market_title",
                List.of(new SyncShopDataPacket.EntryData(
                        "spirit_grass_bundle",
                        "item.seeking_immortals.spirit_grass",
                        3,
                        2,
                        "item",
                        "item.seeking_immortals.metal_spirit_stone",
                        12,
                        24000L)),
                true);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        SyncShopDataPacket.encode(packet, buffer);
        SyncShopDataPacket decoded = SyncShopDataPacket.decode(buffer);

        assertEquals(packet.shopId(), decoded.shopId());
        assertEquals(packet.titleKey(), decoded.titleKey());
        assertTrue(decoded.openScreen());
        assertEquals(1, decoded.entries().size());
        assertEquals("spirit_grass_bundle", decoded.entries().get(0).id());
        assertEquals(2, decoded.entries().get(0).cost());
        assertEquals(24000L, decoded.entries().get(0).nextRefreshTicks());
    }

    @Test
    void shopActionRoundTrips() {
        ShopActionPacket packet = new ShopActionPacket("buy", "market_herbal_stall", "spirit_grass_bundle");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        ShopActionPacket.encode(packet, buffer);
        ShopActionPacket decoded = ShopActionPacket.decode(buffer);

        assertEquals("buy", decoded.action());
        assertEquals("market_herbal_stall", decoded.shopId());
        assertEquals("spirit_grass_bundle", decoded.entryId());
    }

    @Test
    void syncWorldpackDataRoundTrips() {
        SyncWorldpackDataPacket packet = new SyncWorldpackDataPacket(
                "qinglan_mountains",
                "Qinglan Mountains",
                "mist_cave_trial",
                "Mist Cave",
                "mist_valley_seal_thins",
                "Seal Thins",
                1200L,
                List.of("aura_plus_5", "herb_shop_bonus"),
                List.of(new SyncWorldpackDataPacket.RegionData(
                        "qinglan_mountains", "Qinglan Mountains", "mortal", 1.10D, true, true)),
                List.of(new SyncWorldpackDataPacket.RealmData(
                        "mist_cave_trial", "Mist Cave", "qinglan_mountains", "qi_refining",
                        "item.seeking_immortals.metal_spirit_stone", 0L, true, true, true)),
                true);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        SyncWorldpackDataPacket.encode(packet, buffer);
        SyncWorldpackDataPacket decoded = SyncWorldpackDataPacket.decode(buffer);

        assertEquals(packet.currentRegionId(), decoded.currentRegionId());
        assertEquals(packet.dailyEventId(), decoded.dailyEventId());
        assertEquals(2, decoded.dailyEventEffects().size());
        assertEquals(1, decoded.regions().size());
        assertEquals(1.10D, decoded.regions().get(0).auraMultiplier(), 0.0001D);
        assertEquals("mist_cave_trial", decoded.realms().get(0).id());
        assertTrue(decoded.realms().get(0).active());
        assertTrue(decoded.openScreen());
    }

    @Test
    void worldpackActionRoundTrips() {
        WorldpackActionPacket packet = new WorldpackActionPacket("travel", "qinglan_mountains");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        WorldpackActionPacket.encode(packet, buffer);
        WorldpackActionPacket decoded = WorldpackActionPacket.decode(buffer);

        assertEquals("travel", decoded.action());
        assertEquals("qinglan_mountains", decoded.targetId());
    }

    @Test
    void syncShopDataRejectsOversizedEntryCount() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf("market_herbal_stall", 128);
        buffer.writeUtf("screen.title", 192);
        buffer.writeVarInt(65);

        assertThrows(DecoderException.class, () -> SyncShopDataPacket.decode(buffer));
    }

    @Test
    void syncWorldpackDataRejectsOversizedRegionCount() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        writeMinimalWorldpackHeader(buffer);
        buffer.writeVarInt(33);

        assertThrows(DecoderException.class, () -> SyncWorldpackDataPacket.decode(buffer));
    }

    @Test
    void syncWorldpackDataRejectsOversizedEffectCount() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeUtf("qinglan_mountains", 128);
        buffer.writeUtf("Qinglan", 128);
        buffer.writeUtf("", 128);
        buffer.writeUtf("", 128);
        buffer.writeUtf("event", 128);
        buffer.writeUtf("Event", 128);
        buffer.writeVarLong(20L);
        buffer.writeVarInt(17);

        assertThrows(DecoderException.class, () -> SyncWorldpackDataPacket.decode(buffer));
    }

    @Test
    void actionsRejectOversizedText() {
        FriendlyByteBuf shop = new FriendlyByteBuf(Unpooled.buffer());
        shop.writeUtf("x".repeat(65), 128);
        shop.writeUtf("market", 96);
        shop.writeUtf("entry", 96);

        FriendlyByteBuf worldpack = new FriendlyByteBuf(Unpooled.buffer());
        worldpack.writeUtf("travel", 64);
        worldpack.writeUtf("x".repeat(97), 160);

        assertThrows(DecoderException.class, () -> ShopActionPacket.decode(shop));
        assertThrows(DecoderException.class, () -> WorldpackActionPacket.decode(worldpack));
    }

    private static void writeMinimalWorldpackHeader(FriendlyByteBuf buffer) {
        buffer.writeUtf("qinglan_mountains", 128);
        buffer.writeUtf("Qinglan", 128);
        buffer.writeUtf("", 128);
        buffer.writeUtf("", 128);
        buffer.writeUtf("", 128);
        buffer.writeUtf("", 128);
        buffer.writeVarLong(0L);
        buffer.writeVarInt(0);
    }
}
