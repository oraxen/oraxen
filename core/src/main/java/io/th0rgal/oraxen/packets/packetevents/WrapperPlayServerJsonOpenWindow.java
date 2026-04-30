package io.th0rgal.oraxen.packets.packetevents;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import io.th0rgal.oraxen.utils.AdventureUtils;

@SuppressWarnings("deprecation")
class WrapperPlayServerJsonOpenWindow extends WrapperPlayServerOpenWindow {

    WrapperPlayServerJsonOpenWindow(PacketSendEvent event) {
        super(event);
    }

    String getTitleJson() {
        return AdventureUtils.GSON_SERIALIZER.serialize(getTitle());
    }

    void setTitleJson(String titleJson) {
        setTitle(AdventureUtils.GSON_SERIALIZER.deserialize(titleJson));
    }
}
