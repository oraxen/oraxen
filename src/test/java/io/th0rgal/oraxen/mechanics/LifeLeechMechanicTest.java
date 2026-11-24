package io.th0rgal.oraxen.mechanics;

import com.google.common.base.Function;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.mechanics.provided.combat.lifeleech.LifeLeechMechanic;
import io.th0rgal.oraxen.mechanics.provided.combat.lifeleech.LifeLeechMechanicListener;
import io.th0rgal.oraxen.protection.AntiGriefLib;
import io.th0rgal.oraxen.utils.wrappers.AttributeWrapper;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
class LifeLeechMechanicTest extends MechanicTestSupport {

    private static final String ITEM_ID = "leech_sword";

    @Test
    void addsLeechToTheHitAndHealsByTheHealthItTakes() {
        Player damager = damager(10, maxHealth(20));
        LivingEntity victim = victim(20);
        // 5 damage against armor that blocks 20%, so the hit alone deals 4
        EntityDamageByEntityEvent event = event(damager, victim, 5, 0.2);

        callListener(event, 6);

        assertEquals(11, event.getDamage(), 1e-9);
        assertEquals(8.8, event.getFinalDamage(), 1e-9);
        assertEquals(14.8, healthSetOn(damager), 1e-9);
        verify(victim, never()).setHealth(anyDouble());
    }

    @Test
    void healsNoMoreThanTheHealthTheVictimHasLeft() {
        Player damager = damager(10, maxHealth(20));
        LivingEntity victim = victim(6);
        EntityDamageByEntityEvent event = event(damager, victim, 5, 0.2);

        callListener(event, 6);

        assertEquals(12, healthSetOn(damager), 1e-9);
    }

    @Test
    void clampsHealToMaxHealth() {
        Player damager = damager(18, maxHealth(20));
        EntityDamageByEntityEvent event = event(damager, victim(20), 5, 0);

        callListener(event, 6);

        assertEquals(20, healthSetOn(damager), 1e-9);
    }

    @Test
    void ignoresDamagerWithoutMaxHealthAttribute() {
        Player damager = damager(10, null);
        EntityDamageByEntityEvent event = event(damager, victim(20), 5, 0);

        callListener(event, 6);

        assertEquals(5, event.getDamage(), 1e-9);
        verify(damager, never()).setHealth(anyDouble());
    }

    private void callListener(EntityDamageByEntityEvent event, int amount) {
        LifeLeechMechanic mechanic = mock(LifeLeechMechanic.class);
        when(mechanic.getAmount()).thenReturn(amount);
        MechanicFactory factory = mechanicFactory();
        when(factory.getMechanic(ITEM_ID)).thenReturn(mechanic);

        try (MockedStatic<AntiGriefLib> antiGrief = mockStatic(AntiGriefLib.class);
                MockedStatic<OraxenItems> items = mockStatic(OraxenItems.class)) {
            antiGrief.when(() -> AntiGriefLib.canInteract(any(), any())).thenReturn(true);
            items.when(() -> OraxenItems.getIdByItem(any(ItemStack.class))).thenReturn(ITEM_ID);
            items.when(() -> OraxenItems.exists(anyString())).thenReturn(true);

            new LifeLeechMechanicListener(factory).onCall(event);
        }
    }

    private static EntityDamageByEntityEvent event(Player damager, LivingEntity victim, double damage, double armorReduction) {
        Map<DamageModifier, Double> modifiers = new EnumMap<>(DamageModifier.class);
        modifiers.put(DamageModifier.BASE, damage);
        modifiers.put(DamageModifier.ARMOR, -damage * armorReduction);
        Map<DamageModifier, Function<? super Double, Double>> functions = new EnumMap<>(DamageModifier.class);
        functions.put(DamageModifier.BASE, base -> -0.0);
        functions.put(DamageModifier.ARMOR, base -> -base * armorReduction);
        return new EntityDamageByEntityEvent(damager, victim, DamageCause.ENTITY_ATTACK, mock(DamageSource.class),
                modifiers, functions, false);
    }

    private static double healthSetOn(Player damager) {
        ArgumentCaptor<Double> health = ArgumentCaptor.forClass(Double.class);
        verify(damager).setHealth(health.capture());
        return health.getValue();
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

    private static LivingEntity victim(double health) {
        LivingEntity victim = mock(LivingEntity.class);
        when(victim.getHealth()).thenReturn(health);
        return victim;
    }

    private static AttributeInstance maxHealth(double value) {
        AttributeInstance attribute = mock(AttributeInstance.class);
        when(attribute.getValue()).thenReturn(value);
        return attribute;
    }
}
