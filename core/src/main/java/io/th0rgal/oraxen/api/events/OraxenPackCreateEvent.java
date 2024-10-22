package io.th0rgal.oraxen.api.events;

import io.th0rgal.oraxen.utils.VirtualFile;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Event triggered when the <code>pack.zip</code> file is created.
 * <p>
 * This event is fired when the Oraxen resource pack is generated.
 * It allows developers to retrieve the list of virtual files
 * included in the pack.
 * </p>
 */
public class OraxenPackCreateEvent extends Event {

    /**
     * List of virtual files generated for the pack.
     */
    private final List<VirtualFile> output;

    /**
     * Static event handler list required by the Bukkit API.
     */
    private static final HandlerList HANDLERS = new HandlerList();

    /**
     * Constructor for the {@link OraxenPackCreateEvent} event.
     *
     * @param output the list of virtual files generated for the pack
     */
    public OraxenPackCreateEvent(List<VirtualFile> output) {
        this.output = output;
    }

    /**
     * Retrieves the list of virtual files generated for the pack.
     *
     * @return the list of {@link VirtualFile} included in the pack
     */
    public List<VirtualFile> getOutput() {
        return output;
    }

    /**
     * Retrieves the list of event handlers for this event.
     *
     * @return the list of {@link HandlerList}
     */
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    /**
     * Retrieves the static list of event handlers for this event.
     *
     * @return the static {@link HandlerList}
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
