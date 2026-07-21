package io.th0rgal.oraxen.mechanics;

import io.th0rgal.oraxen.mechanics.provided.misc.custom.fields.CustomEvent;
import io.th0rgal.oraxen.mechanics.provided.misc.custom.listeners.ClickListener;
import io.th0rgal.oraxen.utils.actions.ClickAction;
import org.bukkit.event.block.Action;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class CustomClickListenerTest extends MechanicTestSupport {

    @Test
    void clickEventWithoutParametersDefaultsToAllClicks() throws Exception {
        ClickListener listener = listener("CLICK");

        assertEquals(Set.of(
                Action.RIGHT_CLICK_AIR,
                Action.RIGHT_CLICK_BLOCK,
                Action.LEFT_CLICK_AIR,
                Action.LEFT_CLICK_BLOCK
        ), interactActions(listener));
    }

    @Test
    void clickEventAcceptsCaseInsensitiveParameters() throws Exception {
        ClickListener listener = listener("CLICK:RIGHT:Block");

        assertEquals(Set.of(Action.RIGHT_CLICK_BLOCK), interactActions(listener));
    }

    @Test
    void clickEventRejectsInvalidTargetsWithHelpfulError() {
        assertThrows(IllegalArgumentException.class, () -> listener("CLICK:right:entity"));
    }

    private static ClickListener listener(String event) {
        return new ClickListener("test_item", 0, new CustomEvent(event, false), mock(ClickAction.class));
    }

    @SuppressWarnings("unchecked")
    private static Set<Action> interactActions(ClickListener listener) throws Exception {
        Field field = ClickListener.class.getDeclaredField("interactActions");
        field.setAccessible(true);
        return (Set<Action>) field.get(listener);
    }
}
