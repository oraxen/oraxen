package io.th0rgal.oraxen.utils.input;

import java.util.function.BiPredicate;

import org.bukkit.entity.Player;

public interface InputProvider {

    String LINE = "\\u00B6line";

    String[] getInput();

    InputProvider setMessage(String message);

    void open(Player player);

    InputProvider onRespond(BiPredicate<Player, InputProvider> response);

    InputProvider reopenOnFail(boolean state);

    boolean reopenOnFail();

    boolean hasMultipleLines();

    void close();

}
