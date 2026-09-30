package io.th0rgal.oraxen.commands;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.packets.PacketAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class ReportCommand {

    OraxenCommand getReportCommand() {
        return new OraxenCommand("report")
            .withPermission("oraxen.command.report")
            .executes((sender, args) -> {
                // Get Oraxen version
                String oraxenVersion = OraxenPlugin.get().getPluginMeta().getVersion();

                PacketAdapter packetAdapter = OraxenPlugin.get().getPacketAdapter();
                Plugin protocolPlugin = packetAdapter.getPlugin();
                String packetBackend = packetAdapter.backendName();
                if (protocolPlugin != null)
                    packetBackend += "-" + protocolPlugin.getPluginMeta().getVersion();

                // Get server info
                String serverSoftware = Bukkit.getName();
                String serverVersion = Bukkit.getVersion();

                // Get OS info
                String osName = System.getProperty("os.name");
                String osVersion = System.getProperty("os.version");
                String osArch = System.getProperty("os.arch");

                // Format report
                String report = String.format("""
                        
                        ### System Report
                        **Plugin Versions:**
                        - Oraxen: %s
                        - Packet Backend: %s
                        
                        **Server Information:**
                        - Software: %s
                        - Version: %s
                        
                        **System Information:**
                        - OS: %s
                        - OS Version: %s
                        - Architecture: %s
                        """,
                    oraxenVersion,
                    packetBackend,
                    serverSoftware,
                    serverVersion,
                    osName,
                    osVersion,
                    osArch);

                // Send report to sender
                sender.sendPlainMessage(report);
            });
    }
}
