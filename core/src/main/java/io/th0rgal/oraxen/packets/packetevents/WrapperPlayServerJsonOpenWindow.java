package io.th0rgal.oraxen.packets.packetevents;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

@SuppressWarnings("deprecation")
class WrapperPlayServerJsonOpenWindow extends PacketWrapper<WrapperPlayServerJsonOpenWindow> {

    private int containerId;
    private int type;
    private String titleJson;

    WrapperPlayServerJsonOpenWindow(PacketSendEvent event) {
        super(event);
    }

    @Override
    public void read() {
        containerId = readVarInt();
        type = readVarInt();
        titleJson = readComponentJSON();
    }

    @Override
    public void write() {
        writeVarInt(containerId);
        writeVarInt(type);
        writeComponentJSON(titleJson);
    }

    @Override
    public void copy(WrapperPlayServerJsonOpenWindow wrapper) {
        containerId = wrapper.containerId;
        type = wrapper.type;
        titleJson = wrapper.titleJson;
    }

    String getTitleJson() {
        return titleJson;
    }

    void setTitleJson(String titleJson) {
        this.titleJson = titleJson;
    }
}
