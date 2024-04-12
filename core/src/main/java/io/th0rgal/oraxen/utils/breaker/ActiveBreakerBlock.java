package io.th0rgal.oraxen.utils.breaker;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.scheduler.Task;
import org.bukkit.block.Block;

public class ActiveBreakerBlock {
    private final Task task;
    private final int period;
    private final PacketContainer packet;
    private final Block block;

    public ActiveBreakerBlock(final Task task, final int period, final PacketContainer packet, final Block block) {
        this.task = task;
        this.period = period;
        this.packet = packet;
        this.block = block;
    }


}
