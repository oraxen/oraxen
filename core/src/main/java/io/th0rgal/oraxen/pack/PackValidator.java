package io.th0rgal.oraxen.pack;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.OraxenMeta;
import net.kyori.adventure.key.Key;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.model.Model;

import java.util.*;
import java.util.stream.Collectors;

public class PackValidator {

    private final Set<Key> malformedModels = new LinkedHashSet<>();
    private final Set<Key> malformedTextures = new LinkedHashSet<>();

    public void registerMalformedModel(final Key key) {
        malformedModels.add(key);
    }

    public void registerMalformedTexture(final Key key) {
        malformedTextures.add(key);
    }

    public void validateResourcePack(ResourcePack resourcePack) {
        Collection<Model> defaultModels = DefaultResourcePackExtractor.vanillaResourcePack.models();
        Map<String, PackData> oraxenItemMetas = OraxenItems.getEntries().stream()
                .filter(e -> e.getValue().hasOraxenMeta() && e.getValue().getOraxenMeta().hasPackInfos())
                .collect(Collectors.toMap(Map.Entry::getKey, b -> fromOraxenMeta(b.getValue().getOraxenMeta())));

        for (Model model : defaultModels) {
            model = resourcePack.model(model.key());
            if (model == null) continue;
            List<PackData> packDatas = modelKeyToPackData(model);

        }
    }

    private static PackData fromOraxenMeta(OraxenMeta oraxenMeta) {
        return new PackData(oraxenMeta.modelKey(), oraxenMeta.customModelData());
    }

    private record PackData(Key modelKey, int customModelData){}

    private static List<PackData> modelKeyToPackData(Model model) {
        return model.overrides().stream()
                .flatMap(override -> {
                    Optional<Integer> customModelData = override.predicate().stream()
                            .filter(predicate -> "custom_model_data".equals(predicate.name()))
                            .findFirst()
                            .map(predicate -> (Integer) predicate.value());

                    return customModelData.map(data -> Map.entry(data, override.model())).stream();
                }).map(e -> new PackData(e.getValue(), e.getKey())).toList();
    }
}
