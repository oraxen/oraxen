package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.combat.trident.TridentMechanic;
import io.th0rgal.oraxen.mechanics.provided.combat.trident.TridentMechanicFactory;
import io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent;
import io.th0rgal.oraxen.utils.VirtualFile;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.mockito.Mockito.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TridentMechanicTest extends MechanicTestSupport {
    private ConfigurationSection section() {
        return mechanicSection("trident", "appearance.model", "tridents/amethyst",
                "appearance.thrown-model", "custom:tridents/amethyst_thrown");
    }

    @Test
    void defaultsSecondarySoundsAndNormalizesModelPaths() {
        ConfigurationSection section = section();
        section.set("sounds.throw", "trident.throw.sound");
        section.set("sounds.hit", "trident.hit.sound");
        TridentMechanic mechanic = new TridentMechanic(mechanicFactory(), section);
        assertEquals("minecraft:tridents/amethyst", mechanic.getModel());
        assertEquals(mechanic.getModel(), mechanic.getChargingModel());
        assertEquals("custom:tridents/amethyst_thrown", mechanic.getThrownModel());
        assertEquals(ItemDisplay.ItemDisplayTransform.NONE, mechanic.getTransform());
        assertEquals("trident.hit.sound", mechanic.getGroundSound());
        assertEquals("trident.throw.sound", mechanic.getReturnSound());
        assertNull(mechanic.soundFor("item.trident.riptide_1"));
        assertEquals("trident.hit.sound", mechanic.soundFor("item.trident.hit_ground"));
    }

    @Test
    void readsExplicitTransformAndSounds() {
        ConfigurationSection section = section();
        section.set("appearance.transform", "FIXED");
        section.set("sounds.hit-ground", "custom:ground");
        section.set("sounds.return", "custom:return");
        TridentMechanic mechanic = new TridentMechanic(mechanicFactory(), section);
        assertEquals(ItemDisplay.ItemDisplayTransform.FIXED, mechanic.getTransform());
        assertEquals("custom:ground", mechanic.getGroundSound());
        assertEquals("custom:return", mechanic.getReturnSound());
    }

    @Test
    void generatesOrdinaryGeometryModelsForHandAndFlight() {
        TridentMechanic mechanic = new TridentMechanic(mechanicFactory(), section());
        assertEquals("minecraft:model", mechanic.modelDefinition(false).getAsJsonObject("model").get("type").getAsString());
        assertEquals("minecraft:tridents/amethyst", mechanic.modelDefinition(false).getAsJsonObject("model").get("model").getAsString());
        assertEquals("custom:tridents/amethyst_thrown", mechanic.modelDefinition(true).getAsJsonObject("model").get("model").getAsString());
        assertEquals("oraxen_tridents:test_item", mechanic.getThrownItemModel().toString());
    }

    @Test
    void generatedThrownModelPreservesAnItemWithTheOldSyntheticId() {
        TridentMechanic mechanic = new TridentMechanic(mechanicFactory(), section());
        TridentMechanicFactory factory = mock(TridentMechanicFactory.class);
        when(factory.getItems()).thenReturn(Set.of("test_item"));
        when(factory.getMechanic("test_item")).thenReturn(mechanic);
        VirtualFile existing = new VirtualFile("assets/oraxen/items", "test_item_thrown.json",
                new ByteArrayInputStream(new byte[0]));
        List<VirtualFile> output = new ArrayList<>(List.of(existing));
        OraxenPackGeneratedEvent event = new OraxenPackGeneratedEvent(output);
        doCallRealMethod().when(factory).onPackGenerated(event);
        factory.onPackGenerated(event);
        assertTrue(output.contains(existing));
        VirtualFile held = output.stream().filter(file -> file.getPath()
                .equals("assets/oraxen_tridents/items/test_item_held.json")).findFirst().orElseThrow();
        assertEquals(mechanic.getModel(), held.toJsonObject().getAsJsonObject("model").get("model").getAsString());
        VirtualFile thrown = output.stream().filter(file -> file.getPath()
                .equals("assets/oraxen_tridents/items/test_item.json")).findFirst().orElseThrow();
        assertEquals(mechanic.getThrownModel(), thrown.toJsonObject().getAsJsonObject("model").get("model").getAsString());
    }

    @Test
    void itemReloadDropsOldTridentDefinitionsBeforeReparsing() {
        try (var manager = mockStatic(MechanicsManager.class);
             var version = mockStatic(io.th0rgal.oraxen.utils.VersionUtil.class)) {
            version.when(() -> io.th0rgal.oraxen.utils.VersionUtil.atOrAbove("1.21.4")).thenReturn(true);
            TridentMechanicFactory factory = new TridentMechanicFactory(
                    new org.bukkit.configuration.file.YamlConfiguration().createSection("trident"));
            try {
                factory.parse(section());
                assertNotNull(factory.getMechanic("test_item"));
                factory.clearMechanics();
                List<VirtualFile> output = new ArrayList<>();
                factory.onPackGenerated(new OraxenPackGeneratedEvent(output));
                assertTrue(output.isEmpty());
                assertNull(factory.getMechanic("test_item"));
                factory.parse(section());
                assertNotNull(factory.getMechanic("test_item"));
            } finally {
                factory.onUnregister();
            }
        }
    }

    @Test
    void selectsChargingModelOnlyWhileUsingTheHeldItem() {
        ConfigurationSection section = section();
        section.set("appearance.charging-model", "custom:tridents/amethyst_charging.json");
        TridentMechanic mechanic = new TridentMechanic(mechanicFactory(), section);
        var condition = mechanic.modelDefinition(false).getAsJsonObject("model");
        assertEquals("custom:tridents/amethyst_charging", mechanic.getChargingModel());
        assertEquals("minecraft:condition", condition.get("type").getAsString());
        assertEquals("minecraft:using_item", condition.get("property").getAsString());
        assertEquals(mechanic.getChargingModel(), condition.getAsJsonObject("on_true").get("model").getAsString());
        assertEquals(mechanic.getModel(), condition.getAsJsonObject("on_false").get("model").getAsString());
        assertEquals("minecraft:model", condition.getAsJsonObject("on_true").get("type").getAsString());
        assertEquals(mechanic.getThrownModel(), mechanic.modelDefinition(true).getAsJsonObject("model").get("model").getAsString());
    }

    @Test
    void rejectsInvalidOrBlankChargingModel() {
        ConfigurationSection section = section();
        section.set("appearance.charging-model", "Invalid Path");
        assertThrows(IllegalArgumentException.class, () -> new TridentMechanic(mechanicFactory(), section));
        section.set("appearance.charging-model", " ");
        assertThrows(IllegalArgumentException.class, () -> new TridentMechanic(mechanicFactory(), section));
    }

    @Test
    void rejectsMissingOrInvalidAppearance() {
        ConfigurationSection section = section();
        section.set("appearance.thrown-model", null);
        assertThrows(IllegalArgumentException.class, () -> new TridentMechanic(mechanicFactory(), section));
        section.set("appearance.thrown-model", "Invalid Path");
        assertThrows(IllegalArgumentException.class, () -> new TridentMechanic(mechanicFactory(), section));
    }

    @Test
    void rejectsInvalidSoundsBeforeSendingPackets() {
        ConfigurationSection section = section();
        section.set("sounds.throw", "Invalid sound");
        assertThrows(IllegalArgumentException.class, () -> new TridentMechanic(mechanicFactory(), section));
    }
}
