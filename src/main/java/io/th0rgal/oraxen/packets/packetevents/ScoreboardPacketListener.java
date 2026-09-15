package io.th0rgal.oraxen.packets.packetevents;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective;

public class ScoreboardPacketListener implements PacketListener {
    private final ScoreFormat numberFormat = ScoreFormat.blankScore();

    @Override public void onPacketSend(PacketSendEvent event) {
        if(event.getPacketType() != PacketType.Play.Server.SCOREBOARD_OBJECTIVE) return;
        var wrapper = new WrapperPlayServerScoreboardObjective(event);
        if(wrapper.getMode() == WrapperPlayServerScoreboardObjective.ObjectiveMode.REMOVE) return;
        wrapper.setScoreFormat(numberFormat);
    }
}
