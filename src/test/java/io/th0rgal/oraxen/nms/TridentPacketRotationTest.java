package io.th0rgal.oraxen.nms;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TridentPacketRotationTest {
    public record Rotation(Object position, Object deltaMovement, float yRot, float xRot) {}
    public record LegacyPosition(int id, Rotation values, boolean onGround) {}
    public record Position(int id, Object position, float yRot, float xRot, boolean onGround) {}
    public record Removal(List<Integer> entityIds) {}
    public static class LegacyRemoval {
        public List<Integer> getEntityIds() { return List.of(3, 7); }
    }
    public static class Move {
        private final int entityId;
        public final Object delta;
        public final byte yaw;
        public final byte pitch;
        public final boolean onGround;
        public Move(int id, Object delta, byte yaw, byte pitch, boolean onGround) {
            this.entityId = id;
            this.delta = delta;
            this.yaw = yaw;
            this.pitch = pitch;
            this.onGround = onGround;
        }
        public Object getPositionDelta() { return delta; }
    }
    public record Rot(int entityId, byte yaw, byte pitch, boolean onGround) {}
    public record LegacyMove(int entityId, short x, short y, short z, byte yaw, byte pitch, boolean onGround) {
        public short getXa() { return x; }
        public short getYa() { return y; }
        public short getZa() { return z; }
    }

    public record FloatMove(int entityId, Object delta, float yaw, float pitch, boolean onGround) {
        public Object getPositionDelta() { return delta; }
    }
    public record FloatRot(int entityId, float yaw, float pitch, boolean onGround) {}

    @Test
    void preservesDegreeAnglesInFloatBackedMovementPackets() throws ReflectiveOperationException {
        Object delta = new Object();
        FloatMove converted = (FloatMove) TridentPacketRotation.move(
                new FloatMove(9, delta, 0, 0, false), 9, 135.5f, -120f, false);
        assertSame(delta, converted.delta());
        assertEquals(-135.5f, converted.yaw());
        assertEquals(90f, converted.pitch());
        FloatRot rotation = (FloatRot) TridentPacketRotation.move(
                new FloatRot(9, 0, 0, true), 9, 45.25f, 22.5f, true);
        assertEquals(-45.25f, rotation.yaw());
        assertEquals(-22.5f, rotation.pitch());
        assertTrue(rotation.onGround());
    }

    @Test
    void supportsLegacyRelativeAndRotationOnlyPackets() throws ReflectiveOperationException {
        LegacyMove original = new LegacyMove(9, (short) 100, (short) -20, (short) 4, (byte) 0, (byte) 0, false);
        LegacyMove converted = (LegacyMove) TridentPacketRotation.move(original, 9, 90, 45, false);
        assertEquals(original.x(), converted.x());
        assertEquals(original.y(), converted.y());
        assertEquals(original.z(), converted.z());
        assertEquals(-64, converted.yaw());
        assertEquals(-32, converted.pitch());
        Rot rotation = (Rot) TridentPacketRotation.move(new Rot(9, (byte) 0, (byte) 0, true), 9, 90, 45, true);
        assertEquals(converted.yaw(), rotation.yaw());
        assertEquals(converted.pitch(), rotation.pitch());
    }

    @Test
    void supportsBothRemovalPacketAccessors() throws ReflectiveOperationException {
        assertEquals(List.of(3, 7), TridentPacketRotation.removedEntities(new LegacyRemoval()));
        assertEquals(List.of(3, 7), TridentPacketRotation.removedEntities(new Removal(List.of(3, 7))));
    }

    @Test
    void changesOnlyRotationInRelativeMovement() throws ReflectiveOperationException {
        Object delta = new Object();
        Move original = new Move(42, delta, (byte) 32, (byte) -16, true);
        Move converted = (Move) TridentPacketRotation.move(original, 42, 45, -22.5F, true);
        assertEquals(42, TridentPacketRotation.entityId(converted));
        assertSame(delta, converted.delta);
        assertEquals(-32, converted.yaw);
        assertEquals(16, converted.pitch);
        assertTrue(converted.onGround);
    }

    @Test
    void preservesPositionAndVelocityInOlderMovementPackets() throws ReflectiveOperationException {
        Object coordinates = new Object();
        Object velocity = new Object();
        LegacyPosition original = new LegacyPosition(12, new Rotation(coordinates, velocity, 45, -35), false);
        LegacyPosition converted = (LegacyPosition) TridentPacketRotation.positionSync(original);
        assertEquals(12, converted.id());
        assertSame(coordinates, converted.values().position());
        assertSame(velocity, converted.values().deltaMovement());
        assertEquals(-45, converted.values().yRot());
        assertEquals(35, converted.values().xRot());
        assertEquals(45, original.values().yRot());
    }

    @Test
    void preservesPositionPathAndClampsPitchInNewMovementPackets() throws ReflectiveOperationException {
        Object path = new Object();
        Position original = new Position(7, path, 180, 110, true);
        Position converted = (Position) TridentPacketRotation.positionSync(original);
        assertSame(path, converted.position());
        assertEquals(-180, converted.yRot());
        assertEquals(-90, converted.xRot());
        assertTrue(converted.onGround());
    }
}
