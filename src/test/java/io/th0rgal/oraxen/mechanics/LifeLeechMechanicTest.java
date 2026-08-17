package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.combat.lifeleech.LifeLeechMechanic;
import io.th0rgal.oraxen.mechanics.provided.combat.lifeleech.LifeLeechMechanicListener;
import io.th0rgal.oraxen.protection.AntiGriefLib;
import io.th0rgal.oraxen.utils.wrappers.AttributeWrapper;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LifeLeechMechanicTest extends MechanicTestSupport {

    private static final String ITEM_ID = "leech_sword";

    @Test
    void readsAmount() {
        LifeLeechMechanic mechanic = new LifeLeechMechanic(mechanicFactory(), mechanicSection("lifeleech", "amount", 6));

        assertEquals(6, mechanic.getAmount());
    }

    @Test
    void leechesHealthOnHit() {
        Player damager = damager(10, maxHealth(20));
        LivingEntity victim = victim(20, maxHealth(20));

        callListener(damager, victim, 6);

        verify(damager).setHealth(16);
        verify(victim).setHealth(14);
    }

    @Test
    void clampsToMaxHealthOfBothEntities() {
        Player damager = damager(18, maxHealth(20));
        LivingEntity victim = victim(4, maxHealth(20));

        callListener(damager, victim, 6);

        verify(damager).setHealth(20);
        verify(victim).setHealth(0);
    }

    @Test
    void ignoresEntitiesWithoutMaxHealthAttribute() {
        Player damager = damager(10, null);
        LivingEntity victim = victim(20, maxHealth(20));

        callListener(damager, victim, 6);

        verify(damager, never()).setHealth(anyDouble());
        verify(victim, never()).setHealth(anyDouble());
    }

    private void callListener(Player damager, LivingEntity victim, int amount) {
        LifeLeechMechanic mechanic = mock(LifeLeechMechanic.class);
        when(mechanic.getAmount()).thenReturn(amount);
        MechanicFactory factory = mechanicFactory();
        when(factory.getMechanic(ITEM_ID)).thenReturn(mechanic);

        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamager()).thenReturn(damager);
        when(event.getEntity()).thenReturn(victim);

        try (MockedStatic<AntiGriefLib> antiGrief = mockStatic(AntiGriefLib.class);
                MockedStatic<OraxenItems> items = mockStatic(OraxenItems.class)) {
            antiGrief.when(() -> AntiGriefLib.canInteract(any(), any())).thenReturn(true);
            items.when(() -> OraxenItems.getIdByItem(any(ItemStack.class))).thenReturn(ITEM_ID);
            items.when(() -> OraxenItems.exists(anyString())).thenReturn(true);

            new LifeLeechMechanicListener(factory).onCall(event);
        }
    }

    private static Player damager(double health, AttributeInstance maxHealth) {
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(inventory.getItemInMainHand()).thenReturn(mock(ItemStack.class));

        Player damager = mock(Player.class);
        when(damager.getInventory()).thenReturn(inventory);
        when(damager.getHealth()).thenReturn(health);
        when(damager.getAttribute(AttributeWrapper.MAX_HEALTH)).thenReturn(maxHealth);
        return damager;
    }

    private static LivingEntity victim(double health, AttributeInstance maxHealth) {
        LivingEntity victim = mock(LivingEntity.class);
        when(victim.getHealth()).thenReturn(health);
        when(victim.getAttribute(AttributeWrapper.MAX_HEALTH)).thenReturn(maxHealth);
        return victim;
    }

    private static AttributeInstance maxHealth(double value) {
        AttributeInstance attribute = mock(AttributeInstance.class);
        when(attribute.getValue()).thenReturn(value);
        return attribute;
    }
}
