package io.th0rgal.oraxen.packets.packetevents;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;

@SuppressWarnings("deprecation")
class WrapperPlayServerJsonOpenWindow extends WrapperPlayServerOpenWindow {

    private int containerId;
    private int type;
    private String legacyType;
    private int legacySlots;
    private int horseId;
    private String titleJson;
    private String legacyTitle;
    private boolean useProvidedWindowTitle;

    WrapperPlayServerJsonOpenWindow(PacketSendEvent event) {
        super(event);
    }

    @Override
    public void read() {
        if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_21_2)
                || serverVersion.isOlderThan(ServerVersion.V_1_14)) {
            containerId = readContainerId();
        } else {
            containerId = readVarInt();
        }

        if (serverVersion.isOlderThanOrEquals(ServerVersion.V_1_7_10)) {
            type = readUnsignedByte();
            legacyTitle = readString(32);
            legacySlots = readUnsignedByte();
            useProvidedWindowTitle = readBoolean();
            if (type == 11)
                horseId = readInt();
            return;
        }

        if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_14)) {
            type = readVarInt();
            titleJson = readComponentJSON();
        } else {
            legacyType = readString();
            titleJson = readComponentJSON();
            legacySlots = readUnsignedByte();
            if (legacyType.equals("EntityHorse"))
                horseId = readInt();
        }
    }

    @Override
    public void write() {
        if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_21_2)
                || serverVersion.isOlderThan(ServerVersion.V_1_14)) {
            writeContainerId(containerId);
        } else {
            writeVarInt(containerId);
        }

        if (serverVersion.isOlderThanOrEquals(ServerVersion.V_1_7_10)) {
            writeByte(type);
            writeString(legacyTitle);
            writeByte(legacySlots);
            writeBoolean(useProvidedWindowTitle);
            if (type == 11)
                writeInt(horseId);
            return;
        }

        if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_14)) {
            writeVarInt(type);
            writeComponentJSON(titleJson);
        } else {
            writeString(legacyType);
            writeComponentJSON(titleJson);
            writeByte(legacySlots);
            if (legacyType.equals("EntityHorse"))
                writeInt(horseId);
        }
    }

    @Override
    public void copy(WrapperPlayServerOpenWindow wrapper) {
        containerId = wrapper.getContainerId();
        type = wrapper.getType();
        legacyType = wrapper.getLegacyType();
        legacySlots = wrapper.getLegacySlots();
        horseId = wrapper.getHorseId();
        useProvidedWindowTitle = wrapper.isUseProvidedWindowTitle();

        if (wrapper instanceof WrapperPlayServerJsonOpenWindow jsonWrapper) {
            titleJson = jsonWrapper.titleJson;
            legacyTitle = jsonWrapper.legacyTitle;
        } else {
            titleJson = getSerializers().asJson(wrapper.getTitle());
            legacyTitle = getSerializers().asLegacy(wrapper.getTitle());
        }
    }

    String getTitleJson() {
        return titleJson;
    }

    void setTitleJson(String titleJson) {
        this.titleJson = titleJson;
    }
}
