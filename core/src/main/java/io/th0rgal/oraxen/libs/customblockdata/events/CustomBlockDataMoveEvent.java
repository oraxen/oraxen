package io.th0rgal.oraxen.libs.customblockdata.events;

/*
 * Copyright (c) 2022 Alexander Majka (mfnalex) / JEFF Media GbR
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * If you need help or have any suggestions, feel free to join my Discord and head to #programming-help:
 *
 * Discord: https://discord.jeff-media.com/
 *
 * If you find this library helpful or if you're using it one of your paid plugins, please consider leaving a donation
 * to support the further development of this project :)
 *
 * Donations: https://paypal.me/mfnalex
 */

import io.th0rgal.oraxen.libs.customblockdata.CustomBlockData;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a block with CustomBlockData is moved by a piston to a new location.
 *
 * Blocks with protected CustomBlockData (see {@link CustomBlockData#isProtected()} will not trigger this event, however
 * it is possible that unprotected CustomBlockData will be moved to a destination block with protected CustomBlockData. You have
 * to cancel this event yourself to prevent this.
 */
public class CustomBlockDataMoveEvent extends CustomBlockDataEvent {

    private final @NotNull Block blockTo;

    public CustomBlockDataMoveEvent(@NotNull Plugin plugin, @NotNull Block blockFrom, @NotNull Block blockTo, @NotNull Event bukkitEvent) {
        super(plugin, blockFrom, bukkitEvent);
        this.blockTo = blockTo;
    }

    public @NotNull Block getBlockTo() {
        return blockTo;
    }

}