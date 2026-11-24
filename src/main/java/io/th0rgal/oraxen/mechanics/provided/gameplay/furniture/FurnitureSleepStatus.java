package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import java.util.List;

final class FurnitureSleepStatus {

    record State(boolean spectator, boolean sleeping, boolean deepSleeping, boolean sleepingIgnored) {
    }

    private FurnitureSleepStatus() {
    }

    static boolean enoughSleeping(List<State> states, int percentage) {
        int active = 0;
        int sleeping = 0;
        int deepSleeping = 0;
        for (State state : states) {
            if (state == null || state.spectator()) continue;
            if (state.deepSleeping()) return false;
            active++;
            if (state.sleeping() || state.sleepingIgnored()) sleeping++;
            if (state.sleepingIgnored()) deepSleeping++;
        }
        int needed = Math.max(1, (int) Math.ceil(active * percentage / 100.0));
        return sleeping >= needed && deepSleeping >= needed;
    }
}
