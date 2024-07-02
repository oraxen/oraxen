package io.th0rgal.oraxen.utils.actions.impl.other;

import com.google.common.primitives.Floats;
import io.th0rgal.oraxen.OraxenPlugin;
import me.gabytm.util.actions.actions.Action;
import me.gabytm.util.actions.actions.ActionMeta;
import me.gabytm.util.actions.actions.Context;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class SoundAction extends Action<Player> {

    public static final String IDENTIFIER = "sound";

    private final Sound.Source source = getMeta().getProperty("source", Sound.Source.MASTER, input -> Sound.Source.NAMES.value(input.toLowerCase(Locale.ROOT)));
    private final float volume = getMeta().getProperty("volume", 1f, Floats::tryParse);
    private final float pitch = getMeta().getProperty("pitch", 1f, Floats::tryParse);

    public SoundAction(@NotNull ActionMeta<Player> meta) {
        super(meta);
    }

    @Override
    public void run(@NotNull Player player, @NotNull Context<Player> context) {
        final String parsed = getMeta().getParsedData(player, context);

        try {
            OraxenPlugin.get().getAudience().player(player).playSound(Sound.sound(Key.key(parsed), source, volume, pitch));
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

}
