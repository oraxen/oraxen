package io.th0rgal.oraxen.commands;

import dev.jorel.commandapi.CommandAPICommand;
import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class ReportCommand {

    CommandAPICommand getReportCommand() {
        return new CommandAPICommand("report")
                .withPermission("oraxen.command.report")
                .executes((sender, args) -> {
                    // Get Oraxen version
                    String oraxenVersion = OraxenPlugin.get().getDescription().getVersion();

                    // Get ProtocolLib version
                    Plugin protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
                    String protocolLibVersion = protocolLib != null ? protocolLib.getDescription().getVersion()
                            : "Not installed";

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
                            - ProtocolLib: %s

                            **Server Information:**
                            - Software: %s
                            - Version: %s

                            **System Information:**
                            - OS: %s
                            - OS Version: %s
                            - Architecture: %s
                            """,
                            oraxenVersion,
                            protocolLibVersion,
                            serverSoftware,
                            serverVersion,
                            osName,
                            osVersion,
                            osArch);

                    // Send report to sender
                    sender.sendMessage(report);
                });
    }
}
