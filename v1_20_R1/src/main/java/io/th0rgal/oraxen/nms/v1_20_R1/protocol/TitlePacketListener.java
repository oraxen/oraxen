package io.th0rgal.oraxen.nms.v1_20_R1.protocol;

import io.papermc.paper.adventure.PaperAdventure;
import io.th0rgal.oraxen.utils.AdventureUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;

public class TitlePacketListener {
    public static final PacketListener INSTANCE = new PacketListenerBuilder()
            .add(ClientboundSetTitleTextPacket.class, ((player, textPacket) -> {
                player.connection.send(new ClientboundSetTitleTextPacket(parse(textPacket.getText())));
                return true;
            }))
            .add(ClientboundSetSubtitleTextPacket.class, ((player, textPacket) -> {
                player.connection.send(new ClientboundSetSubtitleTextPacket(parse(textPacket.getText())));
                return true;
            }))
            .add(ClientboundSetActionBarTextPacket.class, ((player, textPacket) -> {
                player.connection.send(new ClientboundSetActionBarTextPacket(parse(textPacket.getText())));
                return true;
            }))
            .build();


    private static Component parse(Component component) {
        return PaperAdventure.asVanilla(AdventureUtils.MINI_MESSAGE.deserialize(PlainTextComponentSerializer.plainText().serialize(PaperAdventure.asAdventure(component))));
    }
}
