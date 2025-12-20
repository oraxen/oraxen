package io.th0rgal.oraxen.compatibilities.provided.mcmmo;

import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityActivateEvent;
import com.gmail.nossr50.events.skills.abilities.McMMOPlayerAbilityDeactivateEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens to mcMMO ability activation events to enhance Oraxen tool effects.
 * <p>
 * When players activate mcMMO super abilities, this listener tracks the active
 * state so that Oraxen mechanics can respond accordingly. For example:
 * <ul>
 *   <li>Super Breaker active: BigMining radius could be increased</li>
 *   <li>Tree Feller active: Custom log blocks break in chains</li>
 *   <li>Green Terra active: Harvesting mechanic radius doubled</li>
 *   <li>Serrated Strikes active: Bleeding mechanic duration extended</li>
 * </ul>
 * <p>
 * Oraxen mechanics can check if an ability is active using {@link #hasActiveAbility(Player, PrimarySkillType)}.
 */
public class McMMOAbilityListener implements Listener {

    // Track active abilities per player
    private final Map<UUID, PrimarySkillType> activeAbilities = new ConcurrentHashMap<>();

    /**
     * Called when a player activates an mcMMO super ability.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAbilityActivate(McMMOPlayerAbilityActivateEvent event) {
        Player player = event.getPlayer();
        PrimarySkillType skill = event.getSkill();

        activeAbilities.put(player.getUniqueId(), skill);
    }

    /**
     * Called when a player's mcMMO super ability deactivates.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAbilityDeactivate(McMMOPlayerAbilityDeactivateEvent event) {
        Player player = event.getPlayer();
        activeAbilities.remove(player.getUniqueId());
    }

    /**
     * Checks if a player has an active mcMMO super ability for the given skill.
     *
     * @param player the player to check
     * @param skill  the skill to check for
     * @return true if the player has an active super ability for this skill
     */
    public boolean hasActiveAbility(Player player, PrimarySkillType skill) {
        PrimarySkillType activeSkill = activeAbilities.get(player.getUniqueId());
        return activeSkill != null && activeSkill == skill;
    }

    /**
     * Checks if a player has any active mcMMO super ability.
     *
     * @param player the player to check
     * @return true if the player has any active super ability
     */
    public boolean hasAnyActiveAbility(Player player) {
        return activeAbilities.containsKey(player.getUniqueId());
    }

    /**
     * Gets the active ability skill for a player, if any.
     *
     * @param player the player to check
     * @return the active skill, or null if no ability is active
     */
    public PrimarySkillType getActiveAbility(Player player) {
        return activeAbilities.get(player.getUniqueId());
    }

    /**
     * Checks if a player has Super Breaker active (Mining ability).
     * Useful for enhancing mining-related Oraxen mechanics.
     *
     * @param player the player to check
     * @return true if Super Breaker is active
     */
    public boolean hasSuperBreakerActive(Player player) {
        return hasActiveAbility(player, PrimarySkillType.MINING);
    }

    /**
     * Checks if a player has Tree Feller active (Woodcutting ability).
     * Useful for enhancing woodcutting-related Oraxen mechanics.
     *
     * @param player the player to check
     * @return true if Tree Feller is active
     */
    public boolean hasTreeFellerActive(Player player) {
        return hasActiveAbility(player, PrimarySkillType.WOODCUTTING);
    }

    /**
     * Checks if a player has Green Terra active (Herbalism ability).
     * Useful for enhancing farming-related Oraxen mechanics.
     *
     * @param player the player to check
     * @return true if Green Terra is active
     */
    public boolean hasGreenTerraActive(Player player) {
        return hasActiveAbility(player, PrimarySkillType.HERBALISM);
    }

    /**
     * Checks if a player has Giga Drill Breaker active (Excavation ability).
     * Useful for enhancing excavation-related Oraxen mechanics.
     *
     * @param player the player to check
     * @return true if Giga Drill Breaker is active
     */
    public boolean hasGigaDrillBreakerActive(Player player) {
        return hasActiveAbility(player, PrimarySkillType.EXCAVATION);
    }

    /**
     * Checks if a player has Serrated Strikes active (Swords ability).
     * Useful for enhancing combat-related Oraxen mechanics like bleeding.
     *
     * @param player the player to check
     * @return true if Serrated Strikes is active
     */
    public boolean hasSerratedStrikesActive(Player player) {
        return hasActiveAbility(player, PrimarySkillType.SWORDS);
    }

    /**
     * Clears all tracked abilities. Called on disable.
     */
    public void clear() {
        activeAbilities.clear();
    }
}
