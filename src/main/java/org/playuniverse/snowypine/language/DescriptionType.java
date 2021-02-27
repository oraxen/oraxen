package org.playuniverse.snowypine.language;

import java.util.function.Function;

import org.playuniverse.snowypine.command.CommandInfo;

public enum DescriptionType {

    USAGE(CommandInfo::getUsageId, CommandInfo::getUsage),
    SIMPLE(CommandInfo::getSimpleDescriptionId, CommandInfo::getSimpleDescription),
    DETAILED(CommandInfo::getDetailedDescriptionId, CommandInfo::getDetailedDescription);

    private final Function<CommandInfo, String> id;
    private final Function<CommandInfo, String> message;

    private DescriptionType(Function<CommandInfo, String> id, Function<CommandInfo, String> message) {
        this.id = id;
        this.message = message;
    }

    public boolean isSimple() {
        return this == SIMPLE;
    }

    public boolean isUsage() {
        return this == USAGE;
    }

    public boolean isDetailed() {
        return this == DETAILED;
    }

    public String getId(CommandInfo info) {
        return id.apply(info);
    }

    public String getMessage(CommandInfo info) {
        return message.apply(info);
    }

}
