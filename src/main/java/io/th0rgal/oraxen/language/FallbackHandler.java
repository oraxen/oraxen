package io.th0rgal.oraxen.language;

import static io.th0rgal.oraxen.language.TranslationType.*;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.event.language.OraxenTranslationEvent;
import io.th0rgal.oraxen.language.Translations.TranslationManager.TranslationStorage;

public class FallbackHandler implements Listener {

    @EventHandler
    public void onLoad(OraxenTranslationEvent event) {

        TranslationStorage storage = event.getStorage();

        for (Message message : Message.values())
            storage.set(message.translationId(), MESSAGE, message.value());

        for (Variable variable : Variable.values())
            storage.set(variable.translationId(), VARIABLE, variable.value());

        for (CommandInfo info : OraxenPlugin.get().getCommandProvider().getInfos()) {
            storage.set(info.getUsageId(), DESCRIPTION, info.getUsage());
            storage.set(info.getSimpleDescriptionId(), DESCRIPTION, info.getSimpleDescription());
            storage.set(info.getDetailedDescriptionId(), DESCRIPTION, info.getDetailedDescription());
        }

    }

}
