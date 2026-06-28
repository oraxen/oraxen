package io.th0rgal.oraxen.mechanics.provided.combat.knockbackstrike;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.mechanics.ConfigProperty;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import io.th0rgal.oraxen.mechanics.MechanicInfo;
import io.th0rgal.oraxen.mechanics.MechanicsManager;
import io.th0rgal.oraxen.mechanics.NestedProperty;
import io.th0rgal.oraxen.mechanics.PropertyType;
import org.bukkit.configuration.ConfigurationSection;

@MechanicInfo(
    category = "combat",
    description = "Knocks back enemies after a certain number of consecutive hits with particle effects"
)
public class KnockbackStrikeMechanicFactory extends MechanicFactory {

    @ConfigProperty(
        type = PropertyType.INTEGER,
        description = "Number of consecutive hits required to trigger knockback",
        defaultValue = "3",
        min = 1,
        max = 20
    )
    public static final String PROP_REQUIRED_HITS = "required_hits";

    @ConfigProperty(
        type = PropertyType.DOUBLE,
        description = "Horizontal knockback force (backward)",
        defaultValue = "2.0",
        min = 0.0,
        max = 10.0
    )
    public static final String PROP_KNOCKBACK_HORIZONTAL = "knockback_horizontal";

    @ConfigProperty(
        type = PropertyType.DOUBLE,
        description = "Vertical knockback force (upward)",
        defaultValue = "0.5",
        min = 0.0,
        max = 5.0
    )
    public static final String PROP_KNOCKBACK_VERTICAL = "knockback_vertical";

    @ConfigProperty(
        type = PropertyType.OBJECT,
        description = "Particle effect configuration",
        nested = {
            @NestedProperty(
                name = "type",
                type = PropertyType.ENUM,
                description = "Particle type to display",
                defaultValue = "CRIT",
                enumValues = {"CRIT", "EXPLOSION_NORMAL", "FLAME", "SPELL_WITCH", "REDSTONE", "CLOUD", "DRAGON_BREATH"}
            ),
            @NestedProperty(
                name = "count",
                type = PropertyType.INTEGER,
                description = "Number of particles to spawn",
                defaultValue = "20",
                min = 1,
                max = 100
            ),
            @NestedProperty(
                name = "spread",
                type = PropertyType.DOUBLE,
                description = "Particle spread radius",
                defaultValue = "0.5",
                min = 0.0,
                max = 5.0
            )
        }
    )
    public static final String PROP_PARTICLE = "particle";

    @ConfigProperty(
        type = PropertyType.BOOLEAN,
        description = "Play sound effect on knockback trigger",
        defaultValue = "true"
    )
    public static final String PROP_PLAY_SOUND = "play_sound";

    @ConfigProperty(
        type = PropertyType.STRING,
        description = "Sound to play on knockback",
        defaultValue = "ENTITY_PLAYER_ATTACK_KNOCKBACK"
    )
    public static final String PROP_SOUND_TYPE = "sound_type";

    @ConfigProperty(
        type = PropertyType.DOUBLE,
        description = "Sound volume",
        defaultValue = "1.0",
        min = 0.0,
        max = 2.0
    )
    public static final String PROP_SOUND_VOLUME = "sound_volume";

    @ConfigProperty(
        type = PropertyType.DOUBLE,
        description = "Sound pitch",
        defaultValue = "1.0",
        min = 0.5,
        max = 2.0
    )
    public static final String PROP_SOUND_PITCH = "sound_pitch";

    @ConfigProperty(
        type = PropertyType.INTEGER,
        description = "Time in ticks before hit counter resets (20 ticks = 1 second)",
        defaultValue = "60",
        min = 20,
        max = 200
    )
    public static final String PROP_RESET_TIME = "reset_time";

    @ConfigProperty(
        type = PropertyType.BOOLEAN,
        description = "Show hit counter (method exists but actionbar is removed)",
        defaultValue = "true"
    )
    public static final String PROP_SHOW_COUNTER = "show_counter";

    public KnockbackStrikeMechanicFactory(ConfigurationSection section) {
        super(section);
        MechanicsManager.registerListeners(OraxenPlugin.get(), getMechanicID(), new KnockbackStrikeMechanicListener(this));
    }

    @Override
    public Mechanic parse(ConfigurationSection itemMechanicConfiguration) {
        KnockbackStrikeMechanic mechanic = new KnockbackStrikeMechanic(this, itemMechanicConfiguration);
        addToImplemented(mechanic);
        return mechanic;
    }
}