package io.th0rgal.oraxen.language;

import static io.th0rgal.oraxen.language.TranslationType.*;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import io.th0rgal.oraxen.command.CommandInfo;
import io.th0rgal.oraxen.command.CommandProvider;
import io.th0rgal.oraxen.language.Translations.TranslationManager.TranslationStorage;

public class FallbackHandler implements Listener {

    @EventHandler
    public void onLoad(FallbackTranslationLoadEvent event) {

        TranslationStorage storage = event.getStorage();

        for (Message message : Message.values())
            storage.set(message.id(), MESSAGE, message.value());

        for (Variable variable : Variable.values())
            storage.set(variable.id(), VARIABLE, variable.value());

        for (CommandInfo info : CommandProvider.INFO_PROVIDER.getInfos()) {
            storage.set(info.getSimpleDescriptionId(), DESCRIPTION, info.getSimpleDescription());
            storage.set(info.getDetailedDescriptionId(), DESCRIPTION, info.getDetailedDescription());
        }

    }

}
