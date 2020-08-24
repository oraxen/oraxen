package io.th0rgal.oraxen.utils.input;

import java.util.function.BiPredicate;

import org.bukkit.entity.Player;

public interface InputProvider {

    public static String LINE = "\\u00B6line";

    public String[] getInput();

    public InputProvider setMessage(String message);

    public void open(Player player);

    public InputProvider onRespond(BiPredicate<Player, InputProvider> response);

    public InputProvider reopenOnFail(boolean state);

    public boolean reopenOnFail();

    public boolean hasMultipleLines();

    public void close();

}
