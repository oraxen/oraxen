package io.th0rgal.oraxen.packets.packetevents;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;

@SuppressWarnings("deprecation")
class WrapperPlayServerJsonComponent extends PacketWrapper<WrapperPlayServerJsonComponent> {

    private String componentJson;

    WrapperPlayServerJsonComponent(PacketSendEvent event) {
        super(event);
    }

    @Override
    public void read() {
        componentJson = readComponentJSON();
    }

    @Override
    public void write() {
        writeComponentJSON(componentJson);
    }

    @Override
    public void copy(WrapperPlayServerJsonComponent wrapper) {
        componentJson = wrapper.componentJson;
    }

    String getComponentJson() {
        return componentJson;
    }

    void setComponentJson(String componentJson) {
        this.componentJson = componentJson;
    }
}
