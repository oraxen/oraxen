package io.th0rgal.oraxen.font.packets;

//public class ScoreboardPacketListener extends PacketAdapter {
//
//    public ScoreboardPacketListener() {
//        super(OraxenPlugin.get(), ListenerPriority.MONITOR, PacketType.Play.Server.SCOREBOARD_OBJECTIVE);
//    }
//
//    @Override
//    public void onPacketSending(PacketEvent event) {
//        PacketContainer packet = event.getPacket();
//        try {
//            if (packet.getIntegers().read(0) == 1) return;
//            packet.getModifier().write(3, new BlankFormat());
//        } catch (Exception ignored) {
//            ignored.printStackTrace();
//        }
//    }
//
//}
