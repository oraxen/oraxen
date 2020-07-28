package io.th0rgal.oraxen.language;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.th0rgal.oraxen.language.Translations.TranslationManager;
import io.th0rgal.oraxen.language.Translations.TranslationManager.TranslationStorage;

public class FallbackTranslationLoadEvent extends Event {

    public static final HandlerList HANDLERS = new HandlerList();
	
	private final TranslationManager manager;
	private final TranslationStorage storage;
	
	public FallbackTranslationLoadEvent(TranslationManager manager, TranslationStorage storage) {
		this.manager = manager;
		this.storage = storage;
	}
	
	/*
	 * 
	 */
	
	public TranslationManager getManager() {
		return manager;
	}
	
	public TranslationStorage getStorage() {
		return storage;
	}

    /*
     * 
     */

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
