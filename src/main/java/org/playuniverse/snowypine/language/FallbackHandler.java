package org.playuniverse.snowypine.language;

import static org.playuniverse.snowypine.language.TranslationType.*;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.playuniverse.snowypine.Snowypine;
import org.playuniverse.snowypine.command.CommandInfo;
import org.playuniverse.snowypine.event.language.SnowyTranslationEvent;
import org.playuniverse.snowypine.language.Translations.TranslationManager.TranslationStorage;

public class FallbackHandler implements Listener {

	@EventHandler
	public void onLoad(SnowyTranslationEvent event) {

		TranslationStorage storage = event.getStorage();

		for (Message message : Message.values()) {
			storage.set(message.translationId(), MESSAGE, message.value());
		}

		for (Variable variable : Variable.values()) {
			storage.set(variable.translationId(), VARIABLE, variable.value());
		}

		for (CommandInfo info : Snowypine.getPlugin().getCommandProvider().getInfos()) {
			storage.set(info.getUsageId(), DESCRIPTION, info.getUsage());
			storage.set(info.getSimpleDescriptionId(), DESCRIPTION, info.getSimpleDescription());
			storage.set(info.getDetailedDescriptionId(), DESCRIPTION, info.getDetailedDescription());
		}

	}

}
