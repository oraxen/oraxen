package io.th0rgal.oraxen.pack;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.slicer.Box;
import io.th0rgal.oraxen.pack.slicer.InputTexture;
import io.th0rgal.oraxen.pack.slicer.OutputTexture;
import io.th0rgal.oraxen.pack.slicer.Slicer;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.metadata.animation.AnimationMeta;
import team.unnamed.creative.metadata.gui.GuiBorder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.UnaryOperator;

public class PackSlicer extends Slicer {

    private static final Box STANDARD_CONTAINER_BOX = new Box(0, 0, 176, 166, 256, 256);
    public static final List<InputTexture> INPUTS;

    public PackSlicer(ResourcePack resourcePack) {
        super(resourcePack);
    }

    public static void processInputs(ResourcePack resourcePack) {
        Logs.logInfo("Slicing gui-textures to 1.20.2-format...");
        try {
            new PackSlicer(resourcePack).process(INPUTS);
            Logs.logSuccess("Sliced gui-textures to 1.20.2-format!");
        } catch (Exception e) {
            if (Settings.DEBUG.toBool()) e.printStackTrace();
            else Logs.logWarning(e.getMessage());
            Logs.logWarning("Failed to properly slice textures for 1.20.2", true);
        }
    }

    private static InputTexture input(String key, OutputTexture... outputs) {
        return (new InputTexture(Key.key(key))).outputs(outputs);
    }

    private static InputTexture copy(String name) {
        Key key = nameToKey("minecraft", name);
        return move(key, key);
    }

    private static InputTexture clip(String name, Box box) {
        return clip(name, name, box);
    }

    private static InputTexture clip(String inputName, String outputName, Box box) {
        Key inputKey = nameToKey("minecraft", inputName);
        Key outputKey = nameToKey("minecraft", outputName);
        Box imageBox = new Box(0, 0, box.totalW(), box.totalH(), box.totalW(), box.totalH());
        return (new InputTexture(inputKey)).outputs((new OutputTexture(outputKey, imageBox)).apply((image) -> {
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            int x = box.scaleX(imageWidth);
            int y = box.scaleY(imageHeight);
            int width = box.scaleW(imageWidth);
            int height = box.scaleH(imageHeight);
            BufferedImage subImage = image.getSubimage(x, y, width, height);
            BufferedImage clippedImage = new BufferedImage(imageWidth, imageHeight, 2);
            clippedImage.getGraphics().drawImage(subImage, x, y, null);
            return clippedImage;
        }));
    }

    private static InputTexture move(Key inputKey, Key outputKey) {
        return (new InputTexture(inputKey)).outputs(new OutputTexture(outputKey, new Box(0, 0, 1, 1, 1, 1)));
    }

    private static InputTexture moveRealmsToMinecraft(String name) {
        return move(nameToKey("realms", name), nameToKey("minecraft", name));
    }

    private static Key nameToKey(String namespace, String name) {
        return Key.key(namespace, "gui/" + name);
    }

    private static UnaryOperator<BufferedImage> flipFrameAxis() {
        return (image) -> {
            int frameWidth = image.getWidth() / 2;
            int frameHeight = image.getHeight();
            BufferedImage newImage = new BufferedImage(frameWidth, frameHeight * 2, 2);
            Graphics graphics = newImage.getGraphics();

            for (int frame = 0; frame < 2; ++frame)
                graphics.drawImage(image.getSubimage(frame * frameWidth, 0, frameWidth, frameHeight), 0, frame * frameHeight, null);

            graphics.dispose();
            return newImage;
        };
    }

    private static UnaryOperator<BufferedImage> spriteExtender() {
        return (image) -> {
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            int x = 3 * imageWidth / 17;
            int y = 4 * imageHeight / 16;
            int width = 26 * imageWidth / 17;
            int height = 26 * imageHeight / 16;
            BufferedImage extendedImage = new BufferedImage(width, height, 2);
            extendedImage.getGraphics().drawImage(image, x, y, null);
            return extendedImage;
        };
    }

    static {
        INPUTS = List.of(
                input("minecraft:gui/chat_tags.png",
                        new OutputTexture("minecraft:gui/sprites/icon/chat_modified.png", new Box(0, 0, 9, 9, 32, 32))
                ),
                input("minecraft:gui/container/creative_inventory/tabs.png",
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/scroller.png", new Box(232, 0, 12, 15, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/scroller_disabled.png", new Box(244, 0, 12, 15, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_top_unselected_1.png", new Box(0, 0, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_top_unselected_2.png", new Box(26, 0, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_top_unselected_3.png", new Box(52, 0, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_top_unselected_4.png", new Box(78, 0, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_top_unselected_5.png", new Box(104, 0, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_top_unselected_6.png", new Box(130, 0, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_top_unselected_7.png", new Box(156, 0, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_top_selected_1.png", new Box(0, 32, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_top_selected_2.png", new Box(26, 32, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_top_selected_3.png", new Box(52, 32, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_top_selected_4.png", new Box(78, 32, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_top_selected_5.png", new Box(104, 32, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_top_selected_6.png", new Box(130, 32, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_top_selected_7.png", new Box(156, 32, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_bottom_unselected_1.png", new Box(0, 64, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_bottom_unselected_2.png", new Box(26, 64, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_bottom_unselected_3.png", new Box(52, 64, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_bottom_unselected_4.png", new Box(78, 64, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_bottom_unselected_5.png", new Box(104, 64, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_bottom_unselected_6.png", new Box(130, 64, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_bottom_unselected_7.png", new Box(156, 64, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_bottom_selected_1.png", new Box(0, 96, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_bottom_selected_2.png", new Box(26, 96, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_bottom_selected_3.png", new Box(52, 96, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_bottom_selected_4.png", new Box(78, 96, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_bottom_selected_5.png", new Box(104, 96, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_bottom_selected_6.png", new Box(130, 96, 26, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/creative_inventory/tab_bottom_selected_7.png", new Box(156, 96, 26, 32, 256, 256))
                ),
                input("minecraft:gui/advancements/tabs.png",
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_above_left_selected.png", new Box(0, 32, 28, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_above_middle_selected.png", new Box(28, 32, 28, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_above_right_selected.png", new Box(56, 32, 28, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_above_left.png", new Box(0, 0, 28, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_above_middle.png", new Box(28, 0, 28, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_above_right.png", new Box(56, 0, 28, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_below_left_selected.png", new Box(84, 32, 28, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_below_middle_selected.png", new Box(112, 32, 28, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_below_right_selected.png", new Box(140, 32, 28, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_below_left.png", new Box(84, 0, 28, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_below_middle.png", new Box(112, 0, 28, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_below_right.png", new Box(140, 0, 28, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_left_top_selected.png", new Box(0, 92, 32, 28, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_left_middle_selected.png", new Box(32, 92, 32, 28, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_left_bottom_selected.png", new Box(64, 92, 32, 28, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_left_top.png", new Box(0, 64, 32, 28, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_left_middle.png", new Box(32, 64, 32, 28, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_left_bottom.png", new Box(64, 64, 32, 28, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_right_top_selected.png", new Box(96, 92, 32, 28, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_right_middle_selected.png", new Box(128, 92, 32, 28, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_right_bottom_selected.png", new Box(160, 92, 32, 28, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_right_top.png", new Box(96, 64, 32, 28, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_right_middle.png", new Box(128, 64, 32, 28, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/tab_right_bottom.png", new Box(160, 64, 32, 28, 256, 256))
                ),
                input("minecraft:gui/checkbox.png",
                        new OutputTexture("minecraft:gui/sprites/widget/checkbox_selected_highlighted.png.png", new Box(20, 20, 20, 20, 64, 64)),
                        new OutputTexture("minecraft:gui/sprites/widget/checkbox_selected.png", new Box(0, 20, 20, 20, 64, 64)),
                        new OutputTexture("minecraft:gui/sprites/widget/checkbox_highlighted.png", new Box(20, 0, 20, 20, 64, 64)),
                        new OutputTexture("minecraft:gui/sprites/widget/checkbox.png", new Box(0, 0, 20, 20, 64, 64))
                ),
                input("minecraft:gui/container/blast_furnace.png",
                        new OutputTexture("minecraft:gui/sprites/container/blast_furnace/lit_progress.png", new Box(176, 0, 14, 14, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/blast_furnace/burn_progress.png", new Box(176, 14, 24, 16, 256, 256))),
                input("realms:gui/realms/expired_icon.png",
                        new OutputTexture("minecraft:gui/sprites/realm_status/expired.png", new Box(0, 0, 10, 28, 10, 28))),
                input("minecraft:gui/server_selection.png",
                        new OutputTexture("minecraft:gui/sprites/server_list/join_highlighted.png", new Box(0, 32, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/join.png", new Box(0, 0, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/move_up_highlighted.png", new Box(96, 32, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/move_up.png", new Box(96, 0, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/move_down_highlighted.png", new Box(64, 32, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/move_down.png", new Box(64, 0, 32, 32, 256, 256))),
                input("minecraft:gui/widgets.png.png",
                        new OutputTexture("minecraft:gui/sprites/widget/button.png", new Box(0, 66, 200, 20, 256, 256)).nineSliceMeta(200, 20, GuiBorder.border(4, 4, 20, 20)),
                        new OutputTexture("minecraft:gui/sprites/widget/button_disabled.png", new Box(0, 46, 200, 20, 256, 256)).nineSliceMeta(200, 20, GuiBorder.border(4, 4, 20, 20)),
                        new OutputTexture("minecraft:gui/sprites/widget/button_highlighted.png", new Box(0, 86, 200, 20, 256, 256)).nineSliceMeta(200, 20, GuiBorder.border(4, 4, 20, 20)),
                        new OutputTexture("minecraft:gui/sprites/widget/locked_button.png", new Box(0, 146, 20, 20, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/widget/locked_button_highlighted.png", new Box(0, 166, 20, 20, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/widget/locked_button_disabled.png", new Box(0, 186, 20, 20, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/widget/unlocked_button.png", new Box(20, 146, 20, 20, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/widget/unlocked_button_highlighted.png", new Box(20, 166, 20, 20, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/widget/unlocked_button_disabled.png", new Box(20, 186, 20, 20, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/hotbar.png", new Box(0, 0, 182, 22, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/hotbar_selection.png", new Box(0, 22, 24, 23, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/hotbar_offhand_left.png", new Box(24, 22, 29, 24, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/hotbar_offhand_right.png", new Box(53, 22, 29, 24, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/icon/draft_report.png", new Box(182, 24, 15, 15, 256, 256))),
                input("realms:gui/realms/cross_icon.png",
                        new OutputTexture("minecraft:gui/sprites/widget/cross_button_highlighted.png", new Box(0, 14, 14, 14, 14, 28)),
                        new OutputTexture("minecraft:gui/sprites/widget/cross_button.png", new Box(0, 0, 14, 14, 14, 28))),
                input("minecraft:gui/info_icon.png",
                        new OutputTexture("minecraft:gui/sprites/icon/info.png", new Box(0, 0, 20, 20, 20, 20))),
                input("minecraft:gui/advancements/widgets.png",
                        (new OutputTexture("minecraft:gui/sprites/advancements/title_box.png", new Box(0, 52, 200, 26, 256, 256))).nineSliceMeta(200, 26, 10),
                        new OutputTexture("minecraft:gui/sprites/advancements/box_obtained.png", new Box(0, 0, 200, 26, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/task_frame_obtained.png", new Box(0, 128, 26, 26, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/challenge_frame_obtained.png", new Box(26, 128, 26, 26, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/goal_frame_obtained.png", new Box(52, 128, 26, 26, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/box_unobtained.png", new Box(0, 26, 200, 26, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/task_frame_unobtained.png", new Box(0, 154, 26, 26, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/challenge_frame_unobtained.png", new Box(26, 154, 26, 26, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/advancements/goal_frame_unobtained.png", new Box(52, 154, 26, 26, 256, 256))),
                input("minecraft:gui/tab_button.png",
                        (new OutputTexture("minecraft:gui/sprites/widget/tab_selected.png", new Box(0, 0, 130, 24, 256, 256))).nineSliceMeta(130, 24, GuiBorder.border(2, 0, 2, 2)),
                        (new OutputTexture("minecraft:gui/sprites/widget/tab.png", new Box(0, 48, 130, 24, 256, 256))).nineSliceMeta(130, 24, GuiBorder.border(2, 0, 2, 2)),
                        (new OutputTexture("minecraft:gui/sprites/widget/tab_selected_highlighted.png", new Box(0, 24, 130, 24, 256, 256))).nineSliceMeta(130, 24, GuiBorder.border(2, 0, 2, 2)),
                        (new OutputTexture("minecraft:gui/sprites/widget/tab_highlighted.png", new Box(0, 72, 130, 24, 256, 256))).nineSliceMeta(130, 24, GuiBorder.border(2, 0, 2, 2))),
                input("minecraft:gui/container/furnace.png",
                        new OutputTexture("minecraft:gui/sprites/container/furnace/lit_progress.png", new Box(176, 0, 14, 14, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/furnace/burn_progress.png", new Box(176, 14, 24, 16, 256, 256))),
                input("minecraft:gui/container/brewing_stand.png",
                        new OutputTexture("minecraft:gui/sprites/container/brewing_stand/fuel_length.png", new Box(176, 29, 18, 4, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/brewing_stand/brew_progress.png", new Box(176, 0, 9, 28, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/brewing_stand/bubbles.png", new Box(185, 0, 12, 29, 256, 256))),
                input("minecraft:gui/sprites/language.png",
                        new OutputTexture("minecraft:gui/sprites/icon/language.png", new Box(0, 0, 15, 15, 15, 15))),
                input("realms:gui/realms/off_icon.png",
                        new OutputTexture("minecraft:gui/sprites/realm_status/closed.png", new Box(0, 0, 10, 28, 10, 28))),
                input("minecraft:gui/resource_packs.png",
                        new OutputTexture("minecraft:gui/sprites/transferable_list/select_highlighted.png", new Box(0, 32, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/transferable_list/select.png", new Box(0, 0, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/transferable_list/unselect_highlighted.png", new Box(32, 32, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/transferable_list/unselect.png", new Box(32, 0, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/transferable_list/move_up_highlighted.png", new Box(96, 32, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/transferable_list/move_up.png", new Box(96, 0, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/transferable_list/move_down_highlighted.png", new Box(64, 32, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/transferable_list/move_down.png", new Box(64, 0, 32, 32, 256, 256))),
                input("realms:gui/realms/user_icon.png",
                        new OutputTexture("minecraft:gui/sprites/player_list/make_operator.png", new Box(0, 0, 8, 7, 8, 14)),
                        new OutputTexture("minecraft:gui/sprites/player_list/make_operator_highlighted.png", new Box(0, 7, 8, 7, 8, 14))),
                input("minecraft:gui/container/beacon.png",
                        new OutputTexture("minecraft:gui/sprites/container/beacon/button_disabled.png", new Box(44, 219, 22, 22, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/beacon/button_selected.png", new Box(22, 219, 22, 22, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/beacon/button_highlighted.png", new Box(66, 219, 22, 22, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/beacon/button.png", new Box(0, 219, 22, 22, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/beacon/confirm.png", new Box(90, 220, 18, 18, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/beacon/cancel.png", new Box(112, 220, 18, 18, 256, 256))),
                input("minecraft:gui/container/bundle_background.png",
                        (new OutputTexture("minecraft:gui/sprites/container/bundle/background.png", new Box(0, 0, 32, 32, 256, 256))).nineSliceMeta(32, 32, 4)),
                input("minecraft:gui/toasts.png",
                        new OutputTexture("minecraft:gui/sprites/toast/advancement.png", new Box(0, 0, 160, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/toast/recipe.png", new Box(0, 32, 160, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/toast/system.png", new Box(0, 64, 160, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/toast/tutorial.png", new Box(0, 96, 160, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/toast/movement_keys.png", new Box(176, 0, 20, 20, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/toast/mouse.png", new Box(196, 0, 20, 20, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/toast/tree.png", new Box(216, 0, 20, 20, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/toast/recipe_book.png", new Box(176, 20, 20, 20, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/toast/wooden_planks.png", new Box(196, 20, 20, 20, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/toast/social_interactions.png", new Box(216, 20, 20, 20, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/toast/right_click.png", new Box(236, 20, 20, 20, 256, 256))),
                input("realms:gui/realms/reject_icon.png",
                        new OutputTexture("minecraft:gui/sprites/pending_invite/reject_highlighted.png", new Box(19, 0, 18, 18, 37, 18)),
                        new OutputTexture("minecraft:gui/sprites/pending_invite/reject.png", new Box(0, 0, 18, 18, 37, 18))),
                input("minecraft:gui/book.png",
                        new OutputTexture("minecraft:gui/sprites/widget/page_forward_highlighted.png", new Box(23, 192, 23, 13, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/widget/page_forward.png", new Box(0, 192, 23, 13, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/widget/page_backward_highlighted.png", new Box(23, 205, 23, 13, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/widget/page_backward.png", new Box(0, 205, 23, 13, 256, 256))),
                input("realms:gui/realms/accept_icon.png",
                        new OutputTexture("minecraft:gui/sprites/pending_invite/accept_highlighted.png", new Box(19, 0, 18, 18, 37, 18)),
                        new OutputTexture("minecraft:gui/sprites/pending_invite/accept.png", new Box(0, 0, 18, 18, 37, 18))),
                input("realms:gui/realms/world_icon.png",
                        new OutputTexture("minecraft:gui/sprites/icon/new_realm.png", new Box(0, 0, 40, 20, 40, 20))),
                input("realms:gui/realms/expires_soon_icon.png",
                        (new OutputTexture("minecraft:gui/sprites/realm_status/expires_soon.png", new Box(0, 0, 20, 28, 20, 28))).animationMeta(AnimationMeta.animation().frameTime(10).height(28)).apply(flipFrameAxis())),
                input("minecraft:gui/report_button.png",
                        new OutputTexture("minecraft:gui/sprites/social_interactions/report_button.png", new Box(0, 0, 20, 20, 64, 64)),
                        new OutputTexture("minecraft:gui/sprites/social_interactions/report_button_disabled.png", new Box(0, 40, 20, 20, 64, 64)),
                        new OutputTexture("minecraft:gui/sprites/social_interactions/report_button_highlighted.png", new Box(0, 20, 20, 20, 64, 64))),
                input("realms:gui/realms/news_notification_mainscreen.png",
                        new OutputTexture("minecraft:gui/sprites/icon/news.png", new Box(0, 0, 40, 40, 40, 40))),
                input("realms:gui/realms/trial_icon.png",
                        (new OutputTexture("minecraft:gui/sprites/icon/trial_available.png", new Box(0, 0, 8, 16, 8, 16))).animationMeta(AnimationMeta.animation().frameTime(20))),
                input("minecraft:gui/container/grindstone.png",
                        new OutputTexture("minecraft:gui/sprites/container/grindstone/error.png", new Box(176, 0, 28, 21, 256, 256))),
                input("realms:gui/realms/restore_icon.png",
                        new OutputTexture("minecraft:gui/sprites/backup/restore.png", new Box(0, 0, 17, 10, 17, 20)),
                        new OutputTexture("minecraft:gui/sprites/backup/restore_highlighted.png", new Box(0, 10, 17, 10, 17, 20))),
                input("minecraft:gui/container/villager2.png",
                        new OutputTexture("minecraft:gui/sprites/container/villager/out_of_stock.png", new Box(311, 0, 28, 21, 512, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/villager/experience_bar_background.png", new Box(0, 186, 102, 5, 512, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/villager/experience_bar_current.png", new Box(0, 191, 102, 5, 512, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/villager/experience_bar_result.png", new Box(0, 181, 102, 5, 512, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/villager/scroller.png", new Box(0, 199, 6, 27, 512, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/villager/scroller_disabled.png", new Box(6, 199, 6, 27, 512, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/villager/trade_arrow_out_of_stock.png", new Box(25, 171, 10, 9, 512, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/villager/trade_arrow.png", new Box(15, 171, 10, 9, 512, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/villager/discount_strikethrough.png", new Box(0, 176, 9, 2, 512, 256))),
                input("realms:gui/realms/link_icons.png",
                        new OutputTexture("minecraft:gui/sprites/icon/link_highlighted.png", new Box(15, 0, 15, 15, 30, 15)),
                        new OutputTexture("minecraft:gui/sprites/icon/link.png", new Box(0, 0, 15, 15, 30, 15))),
                input("realms:gui/realms/trailer_icons.png",
                        new OutputTexture("minecraft:gui/sprites/icon/video_link_highlighted.png", new Box(15, 0, 15, 15, 30, 15)),
                        new OutputTexture("minecraft:gui/sprites/icon/video_link.png", new Box(0, 0, 15, 15, 30, 15))),
                input("realms:gui/realms/slot_frame.png",
                        new OutputTexture("minecraft:gui/sprites/widget/slot_frame.png", new Box(0, 0, 80, 80, 80, 80))),
                input("minecraft:gui/bars.png",
                        new OutputTexture("minecraft:gui/sprites/boss_bar/pink_background.png", new Box(0, 0, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/blue_background.png", new Box(0, 10, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/red_background.png", new Box(0, 20, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/green_background.png", new Box(0, 30, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/yellow_background.png", new Box(0, 40, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/purple_background.png", new Box(0, 50, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/white_background.png", new Box(0, 60, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/pink_progress.png", new Box(0, 5, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/blue_progress.png", new Box(0, 15, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/red_progress.png", new Box(0, 25, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/green_progress.png", new Box(0, 35, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/yellow_progress.png", new Box(0, 45, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/purple_progress.png", new Box(0, 55, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/white_progress.png", new Box(0, 65, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/notched_6_background.png", new Box(0, 80, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/notched_10_background.png", new Box(0, 90, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/notched_12_background.png", new Box(0, 100, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/notched_20_background.png", new Box(0, 110, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/notched_6_progress.png", new Box(0, 85, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/notched_10_progress.png", new Box(0, 95, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/notched_12_progress.png", new Box(0, 105, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/boss_bar/notched_20_progress.png", new Box(0, 115, 182, 5, 256, 256))),
                input("minecraft:gui/container/smithing.png",
                        new OutputTexture("minecraft:gui/sprites/container/smithing/error.png", new Box(176, 0, 28, 21, 256, 256))),
                input("minecraft:gui/container/stonecutter.png",
                        new OutputTexture("minecraft:gui/sprites/container/stonecutter/scroller.png", new Box(176, 0, 12, 15, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/stonecutter/scroller_disabled.png", new Box(188, 0, 12, 15, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/stonecutter/recipe_selected.png", new Box(0, 184, 16, 18, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/stonecutter/recipe_highlighted.png", new Box(0, 202, 16, 18, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/stonecutter/recipe.png", new Box(0, 166, 16, 18, 256, 256))),
                input("minecraft:gui/slider.png",
                        (new OutputTexture("minecraft:gui/sprites/widget/slider_highlighted.png", new Box(0, 20, 200, 20, 256, 256))).nineSliceMeta(200, 20, GuiBorder.border(4, 4, 20, 20)),
                        (new OutputTexture("minecraft:gui/sprites/widget/slider.png", new Box(0, 0, 200, 20, 256, 256))).nineSliceMeta(200, 20, GuiBorder.border(4, 4, 20, 20)),
                        (new OutputTexture("minecraft:gui/sprites/widget/slider_handle_highlighted.png", new Box(0, 60, 200, 20, 256, 256))).nineSliceMeta(200, 20, GuiBorder.border(4, 4, 20, 20)),
                        (new OutputTexture("minecraft:gui/sprites/widget/slider_handle.png", new Box(0, 40, 200, 20, 256, 256))).nineSliceMeta(200, 20, GuiBorder.border(4, 4, 20, 20))),
                input("minecraft:gui/container/gamemode_switcher.png",
                        new OutputTexture("minecraft:gui/sprites/gamemode_switcher/slot.png", new Box(0, 75, 26, 26, 128, 128)),
                        new OutputTexture("minecraft:gui/sprites/gamemode_switcher/selection.png", new Box(26, 75, 26, 26, 128, 128))),
                input("minecraft:gui/world_selection.png",
                        new OutputTexture("minecraft:gui/sprites/world_list/error_highlighted.png", new Box(96, 32, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/world_list/error.png", new Box(96, 0, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/world_list/marked_join_highlighted.png", new Box(32, 32, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/world_list/marked_join.png", new Box(32, 0, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/world_list/warning_highlighted.png", new Box(64, 32, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/world_list/warning.png", new Box(64, 0, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/world_list/join_highlighted.png", new Box(0, 32, 32, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/world_list/join.png", new Box(0, 0, 32, 32, 256, 256))),
                input("realms:gui/realms/op_icon.png",
                        new OutputTexture("minecraft:gui/sprites/player_list/remove_operator.png", new Box(0, 0, 8, 7, 8, 14)),
                        new OutputTexture("minecraft:gui/sprites/player_list/remove_operator_highlighted.png", new Box(0, 7, 8, 7, 8, 14))),
                input("realms:gui/realms/plus_icon.png",
                        new OutputTexture("minecraft:gui/sprites/backup/changes.png", new Box(0, 0, 9, 9, 9, 18)),
                        new OutputTexture("minecraft:gui/sprites/backup/changes_highlighted.png", new Box(0, 9, 9, 9, 9, 18))),
                input("minecraft:gui/container/enchanting_table.png",
                        new OutputTexture("minecraft:gui/sprites/container/enchanting_table/level_1.png", new Box(0, 223, 16, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/enchanting_table/level_2.png", new Box(16, 223, 16, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/enchanting_table/level_3.png", new Box(32, 223, 16, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/enchanting_table/level_1_disabled.png", new Box(0, 239, 16, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/enchanting_table/level_2_disabled.png", new Box(16, 239, 16, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/enchanting_table/level_3_disabled.png", new Box(32, 239, 16, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/enchanting_table/enchantment_slot_disabled.png", new Box(0, 185, 108, 19, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/enchanting_table/enchantment_slot_highlighted.png", new Box(0, 204, 108, 19, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/enchanting_table/enchantment_slot.png", new Box(0, 166, 108, 19, 256, 256))),
                input("minecraft:gui/checkmark.png",
                        new OutputTexture("minecraft:gui/sprites/icon/checkmark.png", new Box(0, 0, 9, 8, 9, 8))),
                input("minecraft:gui/container/bundle.png",
                        new OutputTexture("minecraft:gui/sprites/container/bundle/blocked_slot.png", new Box(0, 40, 18, 20, 128, 128)),
                        new OutputTexture("minecraft:gui/sprites/container/bundle/slot.png", new Box(0, 0, 18, 20, 128, 128))),
                input("minecraft:gui/container/horse.png",
                        new OutputTexture("minecraft:gui/sprites/container/horse/chest_slots.png", new Box(0, 166, 90, 54, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/horse/saddle_slot.png", new Box(18, 220, 18, 18, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/horse/llama_armor_slot.png", new Box(36, 220, 18, 18, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/horse/armor_slot.png", new Box(0, 220, 18, 18, 256, 256))),
                input("realms:gui/realms/on_icon.png",
                        new OutputTexture("minecraft:gui/sprites/realm_status/open.png", new Box(0, 0, 10, 28, 10, 28))),
                input("minecraft:gui/container/anvil.png",
                        new OutputTexture("minecraft:gui/sprites/container/anvil/text_field.png", new Box(0, 166, 110, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/anvil/text_field_disabled.png", new Box(0, 182, 110, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/anvil/error.png", new Box(176, 0, 28, 21, 256, 256))),
                input("realms:gui/realms/news_icon.png",
                        new OutputTexture("minecraft:gui/sprites/widget/news_button_highlighted.png", new Box(20, 0, 20, 20, 40, 20)),
                        new OutputTexture("minecraft:gui/sprites/widget/news_button.png", new Box(0, 0, 20, 20, 40, 20))),
                input("realms:gui/realms/invitation_icons.png",
                        new OutputTexture("minecraft:gui/sprites/notification/1.png", new Box(0, 0, 8, 8, 48, 16)),
                        new OutputTexture("minecraft:gui/sprites/notification/2.png", new Box(8, 0, 8, 8, 48, 16)),
                        new OutputTexture("minecraft:gui/sprites/notification/3.png", new Box(16, 0, 8, 8, 48, 16)),
                        new OutputTexture("minecraft:gui/sprites/notification/4.png", new Box(24, 0, 8, 8, 48, 16)),
                        new OutputTexture("minecraft:gui/sprites/notification/5.png", new Box(32, 0, 8, 8, 48, 16)),
                        new OutputTexture("minecraft:gui/sprites/notification/more.png", new Box(40, 0, 8, 8, 48, 16))),
                input("minecraft:gui/recipe_book.png",
                        new OutputTexture("minecraft:gui/sprites/recipe_book/furnace_filter_enabled.png", new Box(180, 182, 26, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/furnace_filter_disabled.png", new Box(152, 182, 26, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/furnace_filter_enabled_highlighted.png", new Box(180, 200, 26, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/furnace_filter_disabled_highlighted.png", new Box(152, 200, 26, 16, 256, 256)),
                        (new OutputTexture("minecraft:gui/sprites/recipe_book/overlay_recipe.png", new Box(82, 208, 32, 32, 256, 256))).nineSliceMeta(32, 32, 4),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/furnace_overlay_highlighted.png", new Box(152, 156, 24, 24, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/furnace_overlay.png", new Box(152, 130, 24, 24, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/crafting_overlay_highlighted.png", new Box(152, 104, 24, 24, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/crafting_overlay.png", new Box(152, 78, 24, 24, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/furnace_overlay_disabled_highlighted.png", new Box(178, 156, 24, 24, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/furnace_overlay_disabled.png", new Box(178, 130, 24, 24, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/crafting_overlay_disabled_highlighted.png", new Box(178, 104, 24, 24, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/crafting_overlay_disabled.png", new Box(178, 78, 24, 24, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/filter_enabled.png", new Box(180, 41, 26, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/filter_disabled.png", new Box(152, 41, 26, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/filter_enabled_highlighted.png", new Box(180, 59, 26, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/filter_disabled_highlighted.png", new Box(152, 59, 26, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/page_forward.png", new Box(1, 208, 12, 17, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/page_forward_highlighted.png", new Box(1, 226, 12, 17, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/page_backward.png", new Box(14, 208, 12, 17, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/page_backward_highlighted.png", new Box(14, 226, 12, 17, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/tab.png", new Box(153, 2, 35, 27, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/tab_selected.png", new Box(188, 2, 35, 27, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/slot_many_craftable.png", new Box(29, 231, 25, 25, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/slot_craftable.png", new Box(29, 206, 25, 25, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/slot_many_uncraftable.png", new Box(54, 231, 25, 25, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/slot_uncraftable.png", new Box(54, 206, 25, 25, 256, 256))),
                input("minecraft:gui/social_interactions.png",
                        new OutputTexture("minecraft:gui/sprites/social_interactions/mute_button.png", new Box(0, 38, 20, 20, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/social_interactions/mute_button_highlighted.png", new Box(0, 58, 20, 20, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/social_interactions/unmute_button.png", new Box(20, 38, 20, 20, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/social_interactions/unmute_button_highlighted.png", new Box(20, 58, 20, 20, 256, 256)),
                        (new OutputTexture("minecraft:gui/sprites/social_interactions/background.png", new Box(1, 1, 236, 34, 256, 256))).nineSliceMeta(236, 34, 8),
                        new OutputTexture("minecraft:gui/sprites/icon/search.png", new Box(243, 1, 12, 12, 256, 256))),
                input("minecraft:gui/spectator_widgets.png",
                        new OutputTexture("minecraft:gui/sprites/spectator/teleport_to_player.png", new Box(0, 0, 16, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/spectator/teleport_to_team.png", new Box(16, 0, 16, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/spectator/close.png", new Box(128, 0, 16, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/spectator/scroll_left.png", new Box(144, 0, 16, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/spectator/scroll_right.png", new Box(160, 0, 16, 16, 256, 256))),
                input("minecraft:gui/container/smoker.png",
                        new OutputTexture("minecraft:gui/sprites/container/smoker/lit_progress.png", new Box(176, 0, 14, 14, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/smoker/burn_progress.png", new Box(176, 14, 24, 16, 256, 256))),
                input("minecraft:gui/recipe_button.png",
                        new OutputTexture("minecraft:gui/sprites/recipe_book/button.png", new Box(0, 0, 20, 18, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/recipe_book/button_highlighted.png", new Box(0, 19, 20, 18, 256, 256))),
                input("minecraft:gui/icons.png",
                        new OutputTexture("minecraft:gui/sprites/icon/ping_unknown.png", new Box(0, 216, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/icon/ping_5.png", new Box(0, 176, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/icon/ping_4.png", new Box(0, 184, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/icon/ping_3.png", new Box(0, 192, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/icon/ping_2.png", new Box(0, 200, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/icon/ping_1.png", new Box(0, 208, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/container_blinking.png", new Box(25, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/container.png", new Box(16, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/full_blinking.png", new Box(70, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/half_blinking.png", new Box(79, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/absorbing_full_blinking.png", new Box(160, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/full.png", new Box(52, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/absorbing_half_blinking.png", new Box(169, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/half.png", new Box(61, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/crosshair.png", new Box(0, 0, 15, 15, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/crosshair_attack_indicator_full.png", new Box(68, 94, 16, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/crosshair_attack_indicator_background.png", new Box(36, 94, 16, 4, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/crosshair_attack_indicator_progress.png", new Box(52, 94, 16, 4, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/hotbar_attack_indicator_background.png", new Box(0, 94, 18, 18, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/hotbar_attack_indicator_progress.png", new Box(18, 94, 18, 18, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/jump_bar_background.png", new Box(0, 84, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/jump_bar_cooldown.png", new Box(0, 74, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/jump_bar_progress.png", new Box(0, 89, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/experience_bar_background.png", new Box(0, 64, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/experience_bar_progress.png", new Box(0, 69, 182, 5, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/armor_full.png", new Box(34, 9, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/armor_half.png", new Box(25, 9, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/armor_empty.png", new Box(16, 9, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/food_empty_hunger.png", new Box(133, 27, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/food_half_hunger.png", new Box(97, 27, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/food_full_hunger.png", new Box(88, 27, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/food_empty.png", new Box(16, 27, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/food_half.png", new Box(61, 27, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/food_full.png", new Box(52, 27, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/air.png", new Box(16, 18, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/air_bursting.png", new Box(25, 18, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/container_hardcore.png", new Box(16, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/container_hardcore_blinking.png", new Box(25, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/hardcore_full.png", new Box(52, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/hardcore_full_blinking.png", new Box(70, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/hardcore_half.png", new Box(61, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/hardcore_half_blinking.png", new Box(79, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/poisoned_full.png", new Box(88, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/poisoned_full_blinking.png", new Box(106, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/poisoned_half.png", new Box(97, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/poisoned_half_blinking.png", new Box(115, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/poisoned_hardcore_full.png", new Box(88, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/poisoned_hardcore_full_blinking.png", new Box(106, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/poisoned_hardcore_half.png", new Box(97, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/poisoned_hardcore_half_blinking.png", new Box(115, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/withered_full.png", new Box(124, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/withered_full_blinking.png", new Box(142, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/withered_half.png", new Box(133, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/withered_half_blinking.png", new Box(151, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/withered_hardcore_full.png", new Box(124, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/withered_hardcore_full_blinking.png", new Box(142, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/withered_hardcore_half.png", new Box(133, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/withered_hardcore_half_blinking.png", new Box(151, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/absorbing_full.png", new Box(160, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/absorbing_half.png", new Box(169, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/absorbing_hardcore_full.png", new Box(160, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/absorbing_hardcore_full_blinking.png", new Box(160, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/absorbing_hardcore_half.png", new Box(169, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/absorbing_hardcore_half_blinking.png", new Box(169, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/frozen_full.png", new Box(178, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/frozen_full_blinking.png", new Box(178, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/frozen_half.png", new Box(187, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/frozen_half_blinking.png", new Box(187, 0, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/frozen_hardcore_full.png", new Box(178, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/frozen_hardcore_full_blinking.png", new Box(178, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/frozen_hardcore_half.png", new Box(187, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/frozen_hardcore_half_blinking.png", new Box(187, 45, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/vehicle_container.png", new Box(52, 9, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/vehicle_full.png", new Box(88, 9, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/heart/vehicle_half.png", new Box(97, 9, 9, 9, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/incompatible.png", new Box(0, 216, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/unreachable.png", new Box(0, 216, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/ping_5.png", new Box(0, 176, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/ping_4.png", new Box(0, 184, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/ping_3.png", new Box(0, 192, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/ping_2.png", new Box(0, 200, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/ping_1.png", new Box(0, 208, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/pinging_1.png", new Box(10, 176, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/pinging_2.png", new Box(10, 184, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/pinging_3.png", new Box(10, 192, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/pinging_4.png", new Box(10, 200, 10, 8, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/server_list/pinging_5.png", new Box(10, 208, 10, 8, 256, 256))),
                input("minecraft:gui/container/inventory.png",
                        new OutputTexture("minecraft:gui/sprites/hud/effect_background_ambient.png", new Box(165, 166, 24, 24, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/hud/effect_background.png", new Box(141, 166, 24, 24, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/inventory/effect_background_large.png", new Box(0, 166, 120, 32, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/inventory/effect_background_small.png", new Box(0, 198, 32, 32, 256, 256))),
                input("minecraft:gui/unseen_notification.png",
                        new OutputTexture("minecraft:gui/sprites/icon/unseen_notification.png", new Box(0, 0, 10, 10, 10, 10))),
                input("minecraft:gui/container/loom.png",
                        new OutputTexture("minecraft:gui/sprites/container/loom/banner_slot.png", new Box(176, 0, 16, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/loom/dye_slot.png", new Box(192, 0, 16, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/loom/pattern_slot.png", new Box(208, 0, 16, 16, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/loom/scroller.png", new Box(232, 0, 12, 15, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/loom/scroller_disabled.png", new Box(244, 0, 12, 15, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/loom/pattern_selected.png", new Box(0, 180, 14, 14, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/loom/pattern_highlighted.png", new Box(0, 194, 14, 14, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/loom/pattern.png", new Box(0, 166, 14, 14, 256, 256)),
                        (new OutputTexture("minecraft:gui/sprites/container/loom/error.png", new Box(176, 17, 17, 16, 256, 256))).apply(spriteExtender())),
                input("realms:gui/realms/invite_icon.png",
                        new OutputTexture("minecraft:gui/sprites/icon/invite.png", new Box(0, 0, 18, 15, 18, 30)),
                        new OutputTexture("minecraft:gui/sprites/icon/invite_highlighted.png", new Box(0, 15, 18, 15, 18, 30))),
                input("minecraft:gui/container/cartography_table.png",
                        new OutputTexture("minecraft:gui/sprites/container/cartography_table/error.png", new Box(226, 132, 28, 21, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/cartography_table/scaled_map.png", new Box(176, 66, 66, 66, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/cartography_table/duplicated_map.png", new Box(176, 132, 50, 66, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/cartography_table/map.png", new Box(176, 0, 66, 66, 256, 256)),
                        new OutputTexture("minecraft:gui/sprites/container/cartography_table/locked.png", new Box(52, 214, 10, 14, 256, 256))),
                input("realms:gui/realms/cross_player_icon.png",
                        new OutputTexture("minecraft:gui/sprites/player_list/remove_player.png", new Box(0, 0, 8, 7, 8, 14)),
                        new OutputTexture("minecraft:gui/sprites/player_list/remove_player_highlighted.png", new Box(0, 7, 8, 7, 8, 14))),
                input("minecraft:gui/sprites/accessibility.png",
                        new OutputTexture("minecraft:gui/sprites/icon/accessibility.png", new Box(0, 0, 15, 15, 15, 15))),
                input("minecraft:gui/container/stats_icons.png",
                        new OutputTexture("minecraft:gui/sprites/container/slot.png", new Box(0, 0, 18, 18, 128, 128)),
                        new OutputTexture("minecraft:gui/sprites/statistics/block_mined.png", new Box(54, 18, 18, 18, 128, 128)),
                        new OutputTexture("minecraft:gui/sprites/statistics/item_broken.png", new Box(72, 18, 18, 18, 128, 128)),
                        new OutputTexture("minecraft:gui/sprites/statistics/item_crafted.png", new Box(18, 18, 18, 18, 128, 128)),
                        new OutputTexture("minecraft:gui/sprites/statistics/item_used.png", new Box(36, 18, 18, 18, 128, 128)),
                        new OutputTexture("minecraft:gui/sprites/statistics/item_picked_up.png", new Box(90, 18, 18, 18, 128, 128)),
                        new OutputTexture("minecraft:gui/sprites/statistics/item_dropped.png", new Box(108, 18, 18, 18, 128, 128)),
                        new OutputTexture("minecraft:gui/sprites/statistics/header.png", new Box(0, 18, 18, 18, 128, 128)),
                        new OutputTexture("minecraft:gui/sprites/statistics/sort_up.png", new Box(36, 0, 18, 18, 128, 128)),
                        new OutputTexture("minecraft:gui/sprites/statistics/sort_down", new Box(18, 0, 18, 18, 128, 128))), moveRealmsToMinecraft("realms/adventure"), moveRealmsToMinecraft("realms/darken"), moveRealmsToMinecraft("realms/empty_frame"), moveRealmsToMinecraft("realms/experience"), moveRealmsToMinecraft("realms/inspiration"), moveRealmsToMinecraft("realms/new_world"), moveRealmsToMinecraft("realms/popup"), moveRealmsToMinecraft("realms/survival_spawn"), moveRealmsToMinecraft("realms/upload"), moveRealmsToMinecraft("title/realms"), copy("advancements/backgrounds/adventure"), copy("advancements/backgrounds/end"), copy("advancements/backgrounds/husbandry"), copy("advancements/backgrounds/nether"), copy("advancements/backgrounds/stone"), copy("advancements/backgrounds/window"), copy("advancements/window"), copy("container/creative_inventory/tab_inventory"), copy("container/creative_inventory/tab_item_search"), copy("container/creative_inventory/tab_items"), clip("container/anvil", STANDARD_CONTAINER_BOX), clip("container/beacon", new Box(0, 0, 230, 219, 256, 256)), clip("container/blast_furnace", STANDARD_CONTAINER_BOX), clip("container/brewing_stand", STANDARD_CONTAINER_BOX), clip("container/cartography_table", STANDARD_CONTAINER_BOX), clip("container/crafting_table", STANDARD_CONTAINER_BOX), clip("container/dispenser", STANDARD_CONTAINER_BOX), clip("container/enchanting_table", STANDARD_CONTAINER_BOX), clip("container/furnace", STANDARD_CONTAINER_BOX), clip("container/gamemode_switcher", new Box(0, 0, 125, 75, 128, 128)), copy("container/generic_54"), clip("container/grindstone", STANDARD_CONTAINER_BOX), clip("container/hopper", new Box(0, 0, 176, 133, 256, 256)), clip("container/horse", STANDARD_CONTAINER_BOX), clip("container/inventory", STANDARD_CONTAINER_BOX), clip("container/loom", STANDARD_CONTAINER_BOX), clip("container/shulker_box", STANDARD_CONTAINER_BOX), clip("container/smithing", STANDARD_CONTAINER_BOX), clip("container/smoker", STANDARD_CONTAINER_BOX), clip("container/stonecutter", STANDARD_CONTAINER_BOX), clip("container/villager2", "container/villager", new Box(0, 0, 276, 166, 512, 256)), copy("hanging_signs/acacia"), copy("hanging_signs/bamboo"), copy("hanging_signs/birch"), copy("hanging_signs/cherry"), copy("hanging_signs/crimson"), copy("hanging_signs/dark_oak"), copy("hanging_signs/jungle"), copy("hanging_signs/mangrove"), copy("hanging_signs/oak"), copy("hanging_signs/spruce"), copy("hanging_signs/warped"), copy("presets/isles"), copy("title/background/panorama_0"), copy("title/background/panorama_1"), copy("title/background/panorama_2"), copy("title/background/panorama_3"), copy("title/background/panorama_4"), copy("title/background/panorama_5"), copy("title/background/panorama_overlay"), copy("title/edition"), copy("title/minceraft"), copy("title/minecraft"), copy("title/mojangstudios"), clip("book", new Box(20, 1, 146, 180, 256, 256)), copy("demo_background"), copy("footer_separator"), copy("header_separator"), copy("light_dirt_background"), copy("options_background"), clip("recipe_book.png", new Box(0, 0, 149, 168, 256, 256)),
                input("assets/minecraft/textures/map/map_icons.png",
                        mapDecoration("player", 0),
                        mapDecoration("frame", 1),
                        mapDecoration("red_marker", 2),
                        mapDecoration("blue_marker", 3),
                        mapDecoration("target_x", 4),
                        mapDecoration("target_point", 5),
                        mapDecoration("player_off_map", 6),
                        mapDecoration("player_off_limits", 7),
                        mapDecoration("woodland_mansion", 8),
                        mapDecoration("ocean_monument", 9),
                        mapDecoration("white_banner", 10),
                        mapDecoration("orange_banner", 11),
                        mapDecoration("magenta_banner", 12),
                        mapDecoration("light_blue_banner", 13),
                        mapDecoration("yellow_banner", 14),
                        mapDecoration("lime_banner", 15),
                        mapDecoration("pink_banner", 16),
                        mapDecoration("gray_banner", 17),
                        mapDecoration("light_gray_banner", 18),
                        mapDecoration("cyan_banner", 19),
                        mapDecoration("purple_banner", 20),
                        mapDecoration("blue_banner", 21),
                        mapDecoration("brown_banner", 22),
                        mapDecoration("green_banner", 23),
                        mapDecoration("red_banner", 24),
                        mapDecoration("black_banner", 25),
                        mapDecoration("red_x", 26),
                        mapDecoration("desert_village", 27),
                        mapDecoration("plains_village", 28),
                        mapDecoration("savanna_village", 29),
                        mapDecoration("snowy_village", 30),
                        mapDecoration("taiga_village", 31),
                        mapDecoration("jungle_temple", 32),
                        mapDecoration("swamp_hut", 33)
                ));
    }

    private static OutputTexture mapDecoration(final String path, final int index) {
        final int x = index % 16;
        final int y = index / 16;
        return new OutputTexture(
                "assets/minecraft/textures/map/decorations/" + path + ".png",
                new Box(x * 8, y * 8, 8, 8, 128, 128)
        );
    }

}
