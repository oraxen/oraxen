package io.th0rgal.oraxen.pack.generation;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.pack.generation.slicer.Box;
import io.th0rgal.oraxen.pack.generation.slicer.InputFile;
import io.th0rgal.oraxen.pack.generation.slicer.OutputFile;
import io.th0rgal.oraxen.pack.generation.slicer.Slicer;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class PackSlicer extends Slicer {

    private static final Path packFolder = OraxenPlugin.get().getResourcePack().getPackFolder().toPath();
    private static final Path assetsFolder = packFolder.resolve("assets/minecraft");
    private static final Box STANDARD_CONTAINER_BOX = new Box(0, 0, 176, 166, 256, 256);
    public static final List<InputFile> INPUTS;
    public static final Set<String> OUTPUT_PATHS;

    public PackSlicer(Path rootPath) {
        super(rootPath, rootPath, null);
    }

    public static void slicePackFiles() {
        if (Settings.DEBUG.toBool()) Logs.logInfo("Slicing gui-textures to 1.20.2-format...");
        try {
            new PackSlicer(packFolder).process(INPUTS);
            if (assetsFolder.toFile().exists()) new PackSlicer(assetsFolder).process(INPUTS);
            if (Settings.DEBUG.toBool()) Logs.logSuccess("Successfully sliced gui-textures for 1.20.2");
        } catch (Exception e) {
            Logs.logWarning("Failed to properly slice textures for 1.20.2");
            if (Settings.DEBUG.toBool()) e.printStackTrace();
        }
    }

    private static InputFile input(String path, OutputFile... outputs) {
        return (new InputFile(path)).outputs(outputs);
    }

    private static InputFile copy(String name) {
        String path = nameToPath("minecraft", name);
        return move(path, path);
    }

    private static InputFile clip(String name, Box box) {
        return clip(name, name, box);
    }

    private static InputFile clip(String inputName, String outputName, Box box) {
        String inputPath = nameToPath("minecraft", inputName);
        String outputPath = nameToPath("minecraft", outputName);
        Box imageBox = new Box(0, 0, box.totalW(), box.totalH(), box.totalW(), box.totalH());
        return (new InputFile(inputPath)).outputs((new OutputFile(outputPath, imageBox)).apply((image) -> {
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

    private static InputFile move(String inputPath, String outputPath) {
        return (new InputFile(inputPath)).outputs(new OutputFile[]{new OutputFile(outputPath, new Box(0, 0, 1, 1, 1, 1))});
    }

    private static InputFile moveRealmsToMinecraft(String name) {
        return move(nameToPath("realms", name), nameToPath("minecraft", name));
    }

    private static String nameToPath(String namespace, String name) {
        return "assets/" + namespace + "/textures/gui/" + name + ".png";
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
                input("textures/gui/chat_tags.png",
                        new OutputFile("textures/gui/sprites/icon/chat_modified.png", new Box(0, 0, 9, 9, 32, 32))
                ),
                input("textures/gui/container/creative_inventory/tabs.png",
                        new OutputFile("textures/gui/sprites/container/creative_inventory/scroller.png", new Box(232, 0, 12, 15, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/scroller_disabled.png", new Box(244, 0, 12, 15, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_top_unselected_1.png", new Box(0, 0, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_top_unselected_2.png", new Box(26, 0, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_top_unselected_3.png", new Box(52, 0, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_top_unselected_4.png", new Box(78, 0, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_top_unselected_5.png", new Box(104, 0, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_top_unselected_6.png", new Box(130, 0, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_top_unselected_7.png", new Box(156, 0, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_top_selected_1.png", new Box(0, 32, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_top_selected_2.png", new Box(26, 32, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_top_selected_3.png", new Box(52, 32, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_top_selected_4.png", new Box(78, 32, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_top_selected_5.png", new Box(104, 32, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_top_selected_6.png", new Box(130, 32, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_top_selected_7.png", new Box(156, 32, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_bottom_unselected_1.png", new Box(0, 64, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_bottom_unselected_2.png", new Box(26, 64, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_bottom_unselected_3.png", new Box(52, 64, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_bottom_unselected_4.png", new Box(78, 64, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_bottom_unselected_5.png", new Box(104, 64, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_bottom_unselected_6.png", new Box(130, 64, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_bottom_unselected_7.png", new Box(156, 64, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_bottom_selected_1.png", new Box(0, 96, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_bottom_selected_2.png", new Box(26, 96, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_bottom_selected_3.png", new Box(52, 96, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_bottom_selected_4.png", new Box(78, 96, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_bottom_selected_5.png", new Box(104, 96, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_bottom_selected_6.png", new Box(130, 96, 26, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/creative_inventory/tab_bottom_selected_7.png", new Box(156, 96, 26, 32, 256, 256))
                ),
                input("textures/gui/advancements/tabs.png",
                        new OutputFile("textures/gui/sprites/advancements/tab_above_left_selected.png", new Box(0, 32, 28, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_above_middle_selected.png", new Box(28, 32, 28, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_above_right_selected.png", new Box(56, 32, 28, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_above_left.png", new Box(0, 0, 28, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_above_middle.png", new Box(28, 0, 28, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_above_right.png", new Box(56, 0, 28, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_below_left_selected.png", new Box(84, 32, 28, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_below_middle_selected.png", new Box(112, 32, 28, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_below_right_selected.png", new Box(140, 32, 28, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_below_left.png", new Box(84, 0, 28, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_below_middle.png", new Box(112, 0, 28, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_below_right.png", new Box(140, 0, 28, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_left_top_selected.png", new Box(0, 92, 32, 28, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_left_middle_selected.png", new Box(32, 92, 32, 28, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_left_bottom_selected.png", new Box(64, 92, 32, 28, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_left_top.png", new Box(0, 64, 32, 28, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_left_middle.png", new Box(32, 64, 32, 28, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_left_bottom.png", new Box(64, 64, 32, 28, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_right_top_selected.png", new Box(96, 92, 32, 28, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_right_middle_selected.png", new Box(128, 92, 32, 28, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_right_bottom_selected.png", new Box(160, 92, 32, 28, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_right_top.png", new Box(96, 64, 32, 28, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_right_middle.png", new Box(128, 64, 32, 28, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/tab_right_bottom.png", new Box(160, 64, 32, 28, 256, 256))
                ),
                input("textures/gui/checkbox.png",
                        new OutputFile("textures/gui/sprites/widget/checkbox_selected_highlighted.png", new Box(20, 20, 20, 20, 64, 64)),
                        new OutputFile("textures/gui/sprites/widget/checkbox_selected.png", new Box(0, 20, 20, 20, 64, 64)),
                        new OutputFile("textures/gui/sprites/widget/checkbox_highlighted.png", new Box(20, 0, 20, 20, 64, 64)),
                        new OutputFile("textures/gui/sprites/widget/checkbox.png", new Box(0, 0, 20, 20, 64, 64))
                ),
                input("textures/gui/container/blast_furnace.png",
                        new OutputFile("textures/gui/sprites/container/blast_furnace/lit_progress.png", new Box(176, 0, 14, 14, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/blast_furnace/burn_progress.png", new Box(176, 14, 24, 16, 256, 256))),
                input("assets/realms/textures/gui/realms/expired_icon.png",
                        new OutputFile("textures/gui/sprites/realm_status/expired.png", new Box(0, 0, 10, 28, 10, 28))),
                input("textures/gui/server_selection.png",
                        new OutputFile("textures/gui/sprites/server_list/join_highlighted.png", new Box(0, 32, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/join.png", new Box(0, 0, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/move_up_highlighted.png", new Box(96, 32, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/move_up.png", new Box(96, 0, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/move_down_highlighted.png", new Box(64, 32, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/move_down.png", new Box(64, 0, 32, 32, 256, 256))),
                input("textures/gui/widgets.png",
                        (new OutputFile("textures/gui/sprites/widget/button.png", new Box(0, 66, 200, 20, 256, 256))).metadata("{\n    \"gui\": {\n        \"scaling\": {\n            \"type\": \"nine_slice\",\n            \"width\": 200,\n            \"height\": 20,\n            \"border\": {\n                \"left\": 20,\n                \"top\": 4,\n                \"right\": 20,\n                \"bottom\": 4\n            }\n        }\n    }\n}\n"),
                        (new OutputFile("textures/gui/sprites/widget/button_disabled.png", new Box(0, 46, 200, 20, 256, 256))).metadata("{\n    \"gui\": {\n        \"scaling\": {\n            \"type\": \"nine_slice\",\n            \"width\": 200,\n            \"height\": 20,\n            \"border\": {\n                \"left\": 20,\n                \"top\": 4,\n                \"right\": 20,\n                \"bottom\": 4\n            }\n        }\n    }\n}\n"),
                        (new OutputFile("textures/gui/sprites/widget/button_highlighted.png", new Box(0, 86, 200, 20, 256, 256))).metadata("{\n    \"gui\": {\n        \"scaling\": {\n            \"type\": \"nine_slice\",\n            \"width\": 200,\n            \"height\": 20,\n            \"border\": {\n                \"left\": 20,\n                \"top\": 4,\n                \"right\": 20,\n                \"bottom\": 4\n            }\n        }\n    }\n}\n"),
                        new OutputFile("textures/gui/sprites/widget/locked_button.png", new Box(0, 146, 20, 20, 256, 256)),
                        new OutputFile("textures/gui/sprites/widget/locked_button_highlighted.png", new Box(0, 166, 20, 20, 256, 256)),
                        new OutputFile("textures/gui/sprites/widget/locked_button_disabled.png", new Box(0, 186, 20, 20, 256, 256)),
                        new OutputFile("textures/gui/sprites/widget/unlocked_button.png", new Box(20, 146, 20, 20, 256, 256)),
                        new OutputFile("textures/gui/sprites/widget/unlocked_button_highlighted.png", new Box(20, 166, 20, 20, 256, 256)),
                        new OutputFile("textures/gui/sprites/widget/unlocked_button_disabled.png", new Box(20, 186, 20, 20, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/hotbar.png", new Box(0, 0, 182, 22, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/hotbar_selection.png", new Box(0, 22, 24, 23, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/hotbar_offhand_left.png", new Box(24, 22, 29, 24, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/hotbar_offhand_right.png", new Box(53, 22, 29, 24, 256, 256)),
                        new OutputFile("textures/gui/sprites/icon/draft_report.png", new Box(182, 24, 15, 15, 256, 256))),
                input("assets/realms/textures/gui/realms/cross_icon.png",
                        new OutputFile("textures/gui/sprites/widget/cross_button_highlighted.png", new Box(0, 14, 14, 14, 14, 28)),
                        new OutputFile("textures/gui/sprites/widget/cross_button.png", new Box(0, 0, 14, 14, 14, 28))),
                input("textures/gui/info_icon.png",
                        new OutputFile("textures/gui/sprites/icon/info.png", new Box(0, 0, 20, 20, 20, 20))),
                input("textures/gui/advancements/widgets.png",
                        (new OutputFile("textures/gui/sprites/advancements/title_box.png", new Box(0, 52, 200, 26, 256, 256))).metadata("{\n    \"gui\": {\n        \"scaling\": {\n            \"type\": \"nine_slice\",\n            \"width\": 200,\n            \"height\": 26,\n            \"border\": 10\n        }\n    }\n}\n"),
                        new OutputFile("textures/gui/sprites/advancements/box_obtained.png", new Box(0, 0, 200, 26, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/task_frame_obtained.png", new Box(0, 128, 26, 26, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/challenge_frame_obtained.png", new Box(26, 128, 26, 26, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/goal_frame_obtained.png", new Box(52, 128, 26, 26, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/box_unobtained.png", new Box(0, 26, 200, 26, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/task_frame_unobtained.png", new Box(0, 154, 26, 26, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/challenge_frame_unobtained.png", new Box(26, 154, 26, 26, 256, 256)),
                        new OutputFile("textures/gui/sprites/advancements/goal_frame_unobtained.png", new Box(52, 154, 26, 26, 256, 256))),
                input("textures/gui/tab_button.png",
                        (new OutputFile("textures/gui/sprites/widget/tab_selected.png", new Box(0, 0, 130, 24, 256, 256))).metadata("{\n    \"gui\": {\n        \"scaling\": {\n            \"type\": \"nine_slice\",\n            \"width\": 130,\n            \"height\": 24,\n            \"border\": {\n                \"left\": 2,\n                \"top\": 2,\n                \"right\": 2,\n                \"bottom\": 0\n            }\n        }\n    }\n}\n"),
                        (new OutputFile("textures/gui/sprites/widget/tab.png", new Box(0, 48, 130, 24, 256, 256))).metadata("{\n    \"gui\": {\n        \"scaling\": {\n            \"type\": \"nine_slice\",\n            \"width\": 130,\n            \"height\": 24,\n            \"border\": {\n                \"left\": 2,\n                \"top\": 2,\n                \"right\": 2,\n                \"bottom\": 0\n            }\n        }\n    }\n}\n"),
                        (new OutputFile("textures/gui/sprites/widget/tab_selected_highlighted.png", new Box(0, 24, 130, 24, 256, 256))).metadata("{\n    \"gui\": {\n        \"scaling\": {\n            \"type\": \"nine_slice\",\n            \"width\": 130,\n            \"height\": 24,\n            \"border\": {\n                \"left\": 2,\n                \"top\": 2,\n                \"right\": 2,\n                \"bottom\": 0\n            }\n        }\n    }\n}\n"),
                        (new OutputFile("textures/gui/sprites/widget/tab_highlighted.png", new Box(0, 72, 130, 24, 256, 256))).metadata("{\n    \"gui\": {\n        \"scaling\": {\n            \"type\": \"nine_slice\",\n            \"width\": 130,\n            \"height\": 24,\n            \"border\": {\n                \"left\": 2,\n                \"top\": 2,\n                \"right\": 2,\n                \"bottom\": 0\n            }\n        }\n    }\n}\n")),
                input("textures/gui/container/furnace.png",
                        new OutputFile("textures/gui/sprites/container/furnace/lit_progress.png", new Box(176, 0, 14, 14, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/furnace/burn_progress.png", new Box(176, 14, 24, 16, 256, 256))),
                input("textures/gui/container/brewing_stand.png",
                        new OutputFile("textures/gui/sprites/container/brewing_stand/fuel_length.png", new Box(176, 29, 18, 4, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/brewing_stand/brew_progress.png", new Box(176, 0, 9, 28, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/brewing_stand/bubbles.png", new Box(185, 0, 12, 29, 256, 256))),
                input("textures/gui/sprites/language.png",
                        new OutputFile("textures/gui/sprites/icon/language.png", new Box(0, 0, 15, 15, 15, 15))),
                input("assets/realms/textures/gui/realms/off_icon.png",
                        new OutputFile("textures/gui/sprites/realm_status/closed.png", new Box(0, 0, 10, 28, 10, 28))),
                input("textures/gui/resource_packs.png",
                        new OutputFile("textures/gui/sprites/transferable_list/select_highlighted.png", new Box(0, 32, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/transferable_list/select.png", new Box(0, 0, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/transferable_list/unselect_highlighted.png", new Box(32, 32, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/transferable_list/unselect.png", new Box(32, 0, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/transferable_list/move_up_highlighted.png", new Box(96, 32, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/transferable_list/move_up.png", new Box(96, 0, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/transferable_list/move_down_highlighted.png", new Box(64, 32, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/transferable_list/move_down.png", new Box(64, 0, 32, 32, 256, 256))),
                input("assets/realms/textures/gui/realms/user_icon.png",
                        new OutputFile("textures/gui/sprites/player_list/make_operator.png", new Box(0, 0, 8, 7, 8, 14)),
                        new OutputFile("textures/gui/sprites/player_list/make_operator_highlighted.png", new Box(0, 7, 8, 7, 8, 14))),
                input("textures/gui/container/beacon.png",
                        new OutputFile("textures/gui/sprites/container/beacon/button_disabled.png", new Box(44, 219, 22, 22, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/beacon/button_selected.png", new Box(22, 219, 22, 22, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/beacon/button_highlighted.png", new Box(66, 219, 22, 22, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/beacon/button.png", new Box(0, 219, 22, 22, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/beacon/confirm.png", new Box(90, 220, 18, 18, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/beacon/cancel.png", new Box(112, 220, 18, 18, 256, 256))),
                input("textures/gui/container/bundle_background.png",
                        (new OutputFile("textures/gui/sprites/container/bundle/background.png", new Box(0, 0, 32, 32, 256, 256))).metadata("{\n    \"gui\": {\n        \"scaling\": {\n            \"type\": \"nine_slice\",\n            \"width\": 32,\n            \"height\": 32,\n            \"border\": 4\n        }\n    }\n}\n")),
                input("textures/gui/toasts.png",
                        new OutputFile("textures/gui/sprites/toast/advancement.png", new Box(0, 0, 160, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/toast/recipe.png", new Box(0, 32, 160, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/toast/system.png", new Box(0, 64, 160, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/toast/tutorial.png", new Box(0, 96, 160, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/toast/movement_keys.png", new Box(176, 0, 20, 20, 256, 256)),
                        new OutputFile("textures/gui/sprites/toast/mouse.png", new Box(196, 0, 20, 20, 256, 256)),
                        new OutputFile("textures/gui/sprites/toast/tree.png", new Box(216, 0, 20, 20, 256, 256)),
                        new OutputFile("textures/gui/sprites/toast/recipe_book.png", new Box(176, 20, 20, 20, 256, 256)),
                        new OutputFile("textures/gui/sprites/toast/wooden_planks.png", new Box(196, 20, 20, 20, 256, 256)),
                        new OutputFile("textures/gui/sprites/toast/social_interactions.png", new Box(216, 20, 20, 20, 256, 256)),
                        new OutputFile("textures/gui/sprites/toast/right_click.png", new Box(236, 20, 20, 20, 256, 256))),
                input("assets/realms/textures/gui/realms/reject_icon.png",
                        new OutputFile("textures/gui/sprites/pending_invite/reject_highlighted.png", new Box(19, 0, 18, 18, 37, 18)),
                        new OutputFile("textures/gui/sprites/pending_invite/reject.png", new Box(0, 0, 18, 18, 37, 18))),
                input("textures/gui/book.png",
                        new OutputFile("textures/gui/sprites/widget/page_forward_highlighted.png", new Box(23, 192, 23, 13, 256, 256)),
                        new OutputFile("textures/gui/sprites/widget/page_forward.png", new Box(0, 192, 23, 13, 256, 256)),
                        new OutputFile("textures/gui/sprites/widget/page_backward_highlighted.png", new Box(23, 205, 23, 13, 256, 256)),
                        new OutputFile("textures/gui/sprites/widget/page_backward.png", new Box(0, 205, 23, 13, 256, 256))),
                input("assets/realms/textures/gui/realms/accept_icon.png",
                        new OutputFile("textures/gui/sprites/pending_invite/accept_highlighted.png", new Box(19, 0, 18, 18, 37, 18)),
                        new OutputFile("textures/gui/sprites/pending_invite/accept.png", new Box(0, 0, 18, 18, 37, 18))),
                input("assets/realms/textures/gui/realms/world_icon.png",
                        new OutputFile("textures/gui/sprites/icon/new_realm.png", new Box(0, 0, 40, 20, 40, 20))),
                input("assets/realms/textures/gui/realms/expires_soon_icon.png",
                        (new OutputFile("textures/gui/sprites/realm_status/expires_soon.png", new Box(0, 0, 20, 28, 20, 28))).metadata("{\n    \"animation\": {\n        \"frametime\": 10,\n        \"height\": 28\n    }\n}\n").apply(flipFrameAxis())),
                input("textures/gui/report_button.png",
                        new OutputFile("textures/gui/sprites/social_interactions/report_button.png", new Box(0, 0, 20, 20, 64, 64)),
                        new OutputFile("textures/gui/sprites/social_interactions/report_button_disabled.png", new Box(0, 40, 20, 20, 64, 64)),
                        new OutputFile("textures/gui/sprites/social_interactions/report_button_highlighted.png", new Box(0, 20, 20, 20, 64, 64))),
                input("assets/realms/textures/gui/realms/news_notification_mainscreen.png",
                        new OutputFile("textures/gui/sprites/icon/news.png", new Box(0, 0, 40, 40, 40, 40))),
                input("assets/realms/textures/gui/realms/trial_icon.png",
                        (new OutputFile("textures/gui/sprites/icon/trial_available.png", new Box(0, 0, 8, 16, 8, 16))).metadata("{\n    \"animation\": {\n        \"frametime\": 20\n    }\n}\n")),
                input("textures/gui/container/grindstone.png",
                        new OutputFile("textures/gui/sprites/container/grindstone/error.png", new Box(176, 0, 28, 21, 256, 256))),
                input("assets/realms/textures/gui/realms/restore_icon.png",
                        new OutputFile("textures/gui/sprites/backup/restore.png", new Box(0, 0, 17, 10, 17, 20)),
                        new OutputFile("textures/gui/sprites/backup/restore_highlighted.png", new Box(0, 10, 17, 10, 17, 20))),
                input("textures/gui/container/villager2.png",
                        new OutputFile("textures/gui/sprites/container/villager/out_of_stock.png", new Box(311, 0, 28, 21, 512, 256)),
                        new OutputFile("textures/gui/sprites/container/villager/experience_bar_background.png", new Box(0, 186, 102, 5, 512, 256)),
                        new OutputFile("textures/gui/sprites/container/villager/experience_bar_current.png", new Box(0, 191, 102, 5, 512, 256)),
                        new OutputFile("textures/gui/sprites/container/villager/experience_bar_result.png", new Box(0, 181, 102, 5, 512, 256)),
                        new OutputFile("textures/gui/sprites/container/villager/scroller.png", new Box(0, 199, 6, 27, 512, 256)),
                        new OutputFile("textures/gui/sprites/container/villager/scroller_disabled.png", new Box(6, 199, 6, 27, 512, 256)),
                        new OutputFile("textures/gui/sprites/container/villager/trade_arrow_out_of_stock.png", new Box(25, 171, 10, 9, 512, 256)),
                        new OutputFile("textures/gui/sprites/container/villager/trade_arrow.png", new Box(15, 171, 10, 9, 512, 256)),
                        new OutputFile("textures/gui/sprites/container/villager/discount_strikethrough.png", new Box(0, 176, 9, 2, 512, 256))),
                input("assets/realms/textures/gui/realms/link_icons.png",
                        new OutputFile("textures/gui/sprites/icon/link_highlighted.png", new Box(15, 0, 15, 15, 30, 15)),
                        new OutputFile("textures/gui/sprites/icon/link.png", new Box(0, 0, 15, 15, 30, 15))),
                input("assets/realms/textures/gui/realms/trailer_icons.png",
                        new OutputFile("textures/gui/sprites/icon/video_link_highlighted.png", new Box(15, 0, 15, 15, 30, 15)),
                        new OutputFile("textures/gui/sprites/icon/video_link.png", new Box(0, 0, 15, 15, 30, 15))),
                input("assets/realms/textures/gui/realms/slot_frame.png",
                        new OutputFile("textures/gui/sprites/widget/slot_frame.png", new Box(0, 0, 80, 80, 80, 80))),
                input("textures/gui/bars.png",
                        new OutputFile("textures/gui/sprites/boss_bar/pink_background.png", new Box(0, 0, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/blue_background.png", new Box(0, 10, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/red_background.png", new Box(0, 20, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/green_background.png", new Box(0, 30, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/yellow_background.png", new Box(0, 40, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/purple_background.png", new Box(0, 50, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/white_background.png", new Box(0, 60, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/pink_progress.png", new Box(0, 5, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/blue_progress.png", new Box(0, 15, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/red_progress.png", new Box(0, 25, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/green_progress.png", new Box(0, 35, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/yellow_progress.png", new Box(0, 45, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/purple_progress.png", new Box(0, 55, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/white_progress.png", new Box(0, 65, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/notched_6_background.png", new Box(0, 80, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/notched_10_background.png", new Box(0, 90, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/notched_12_background.png", new Box(0, 100, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/notched_20_background.png", new Box(0, 110, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/notched_6_progress.png", new Box(0, 85, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/notched_10_progress.png", new Box(0, 95, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/notched_12_progress.png", new Box(0, 105, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/boss_bar/notched_20_progress.png", new Box(0, 115, 182, 5, 256, 256))),
                input("textures/gui/container/smithing.png",
                        new OutputFile("textures/gui/sprites/container/smithing/error.png", new Box(176, 0, 28, 21, 256, 256))),
                input("textures/gui/container/stonecutter.png",
                        new OutputFile("textures/gui/sprites/container/stonecutter/scroller.png", new Box(176, 0, 12, 15, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/stonecutter/scroller_disabled.png", new Box(188, 0, 12, 15, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/stonecutter/recipe_selected.png", new Box(0, 184, 16, 18, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/stonecutter/recipe_highlighted.png", new Box(0, 202, 16, 18, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/stonecutter/recipe.png", new Box(0, 166, 16, 18, 256, 256))),
                input("textures/gui/slider.png",
                        (new OutputFile("textures/gui/sprites/widget/slider_highlighted.png", new Box(0, 20, 200, 20, 256, 256))).metadata("{\n    \"gui\": {\n        \"scaling\": {\n            \"type\": \"nine_slice\",\n            \"width\": 200,\n            \"height\": 20,\n            \"border\": {\n                \"left\": 20,\n                \"top\": 4,\n                \"right\": 20,\n                \"bottom\": 4\n            }\n        }\n    }\n}\n"),
                        (new OutputFile("textures/gui/sprites/widget/slider.png", new Box(0, 0, 200, 20, 256, 256))).metadata("{\n    \"gui\": {\n        \"scaling\": {\n            \"type\": \"nine_slice\",\n            \"width\": 200,\n            \"height\": 20,\n            \"border\": {\n                \"left\": 20,\n                \"top\": 4,\n                \"right\": 20,\n                \"bottom\": 4\n            }\n        }\n    }\n}\n"),
                        (new OutputFile("textures/gui/sprites/widget/slider_handle_highlighted.png", new Box(0, 60, 200, 20, 256, 256))).metadata("{\n    \"gui\": {\n        \"scaling\": {\n            \"type\": \"nine_slice\",\n            \"width\": 200,\n            \"height\": 20,\n            \"border\": {\n                \"left\": 20,\n                \"top\": 4,\n                \"right\": 20,\n                \"bottom\": 4\n            }\n        }\n    }\n}\n"),
                        (new OutputFile("textures/gui/sprites/widget/slider_handle.png", new Box(0, 40, 200, 20, 256, 256))).metadata("{\n    \"gui\": {\n        \"scaling\": {\n            \"type\": \"nine_slice\",\n            \"width\": 200,\n            \"height\": 20,\n            \"border\": {\n                \"left\": 20,\n                \"top\": 4,\n                \"right\": 20,\n                \"bottom\": 4\n            }\n        }\n    }\n}\n")),
                input("textures/gui/container/gamemode_switcher.png",
                        new OutputFile("textures/gui/sprites/gamemode_switcher/slot.png", new Box(0, 75, 26, 26, 128, 128)),
                        new OutputFile("textures/gui/sprites/gamemode_switcher/selection.png", new Box(26, 75, 26, 26, 128, 128))),
                input("textures/gui/world_selection.png",
                        new OutputFile("textures/gui/sprites/world_list/error_highlighted.png", new Box(96, 32, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/world_list/error.png", new Box(96, 0, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/world_list/marked_join_highlighted.png", new Box(32, 32, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/world_list/marked_join.png", new Box(32, 0, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/world_list/warning_highlighted.png", new Box(64, 32, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/world_list/warning.png", new Box(64, 0, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/world_list/join_highlighted.png", new Box(0, 32, 32, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/world_list/join.png", new Box(0, 0, 32, 32, 256, 256))),
                input("assets/realms/textures/gui/realms/op_icon.png",
                        new OutputFile("textures/gui/sprites/player_list/remove_operator.png", new Box(0, 0, 8, 7, 8, 14)),
                        new OutputFile("textures/gui/sprites/player_list/remove_operator_highlighted.png", new Box(0, 7, 8, 7, 8, 14))),
                input("assets/realms/textures/gui/realms/plus_icon.png",
                        new OutputFile("textures/gui/sprites/backup/changes.png", new Box(0, 0, 9, 9, 9, 18)),
                        new OutputFile("textures/gui/sprites/backup/changes_highlighted.png", new Box(0, 9, 9, 9, 9, 18))),
                input("textures/gui/container/enchanting_table.png",
                        new OutputFile("textures/gui/sprites/container/enchanting_table/level_1.png", new Box(0, 223, 16, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/enchanting_table/level_2.png", new Box(16, 223, 16, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/enchanting_table/level_3.png", new Box(32, 223, 16, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/enchanting_table/level_1_disabled.png", new Box(0, 239, 16, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/enchanting_table/level_2_disabled.png", new Box(16, 239, 16, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/enchanting_table/level_3_disabled.png", new Box(32, 239, 16, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/enchanting_table/enchantment_slot_disabled.png", new Box(0, 185, 108, 19, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/enchanting_table/enchantment_slot_highlighted.png", new Box(0, 204, 108, 19, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/enchanting_table/enchantment_slot.png", new Box(0, 166, 108, 19, 256, 256))),
                input("textures/gui/checkmark.png",
                        new OutputFile("textures/gui/sprites/icon/checkmark.png", new Box(0, 0, 9, 8, 9, 8))),
                input("textures/gui/container/bundle.png",
                        new OutputFile("textures/gui/sprites/container/bundle/blocked_slot.png", new Box(0, 40, 18, 20, 128, 128)),
                        new OutputFile("textures/gui/sprites/container/bundle/slot.png", new Box(0, 0, 18, 20, 128, 128))),
                input("textures/gui/container/horse.png",
                        new OutputFile("textures/gui/sprites/container/horse/chest_slots.png", new Box(0, 166, 90, 54, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/horse/saddle_slot.png", new Box(18, 220, 18, 18, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/horse/llama_armor_slot.png", new Box(36, 220, 18, 18, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/horse/armor_slot.png", new Box(0, 220, 18, 18, 256, 256))),
                input("assets/realms/textures/gui/realms/on_icon.png",
                        new OutputFile("textures/gui/sprites/realm_status/open.png", new Box(0, 0, 10, 28, 10, 28))),
                input("textures/gui/container/anvil.png",
                        new OutputFile("textures/gui/sprites/container/anvil/text_field.png", new Box(0, 166, 110, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/anvil/text_field_disabled.png", new Box(0, 182, 110, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/anvil/error.png", new Box(176, 0, 28, 21, 256, 256))),
                input("assets/realms/textures/gui/realms/news_icon.png",
                        new OutputFile("textures/gui/sprites/widget/news_button_highlighted.png", new Box(20, 0, 20, 20, 40, 20)),
                        new OutputFile("textures/gui/sprites/widget/news_button.png", new Box(0, 0, 20, 20, 40, 20))),
                input("assets/realms/textures/gui/realms/invitation_icons.png",
                        new OutputFile("textures/gui/sprites/notification/1.png", new Box(0, 0, 8, 8, 48, 16)),
                        new OutputFile("textures/gui/sprites/notification/2.png", new Box(8, 0, 8, 8, 48, 16)),
                        new OutputFile("textures/gui/sprites/notification/3.png", new Box(16, 0, 8, 8, 48, 16)),
                        new OutputFile("textures/gui/sprites/notification/4.png", new Box(24, 0, 8, 8, 48, 16)),
                        new OutputFile("textures/gui/sprites/notification/5.png", new Box(32, 0, 8, 8, 48, 16)),
                        new OutputFile("textures/gui/sprites/notification/more.png", new Box(40, 0, 8, 8, 48, 16))),
                input("textures/gui/recipe_book.png",
                        new OutputFile("textures/gui/sprites/recipe_book/furnace_filter_enabled.png", new Box(180, 182, 26, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/furnace_filter_disabled.png", new Box(152, 182, 26, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/furnace_filter_enabled_highlighted.png", new Box(180, 200, 26, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/furnace_filter_disabled_highlighted.png", new Box(152, 200, 26, 16, 256, 256)),
                        (new OutputFile("textures/gui/sprites/recipe_book/overlay_recipe.png", new Box(82, 208, 32, 32, 256, 256))).metadata("{\n    \"gui\": {\n        \"scaling\": {\n            \"type\": \"nine_slice\",\n            \"width\": 32,\n            \"height\": 32,\n            \"border\": 4\n        }\n    }\n}\n"),
                        new OutputFile("textures/gui/sprites/recipe_book/furnace_overlay_highlighted.png", new Box(152, 156, 24, 24, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/furnace_overlay.png", new Box(152, 130, 24, 24, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/crafting_overlay_highlighted.png", new Box(152, 104, 24, 24, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/crafting_overlay.png", new Box(152, 78, 24, 24, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/furnace_overlay_disabled_highlighted.png", new Box(178, 156, 24, 24, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/furnace_overlay_disabled.png", new Box(178, 130, 24, 24, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/crafting_overlay_disabled_highlighted.png", new Box(178, 104, 24, 24, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/crafting_overlay_disabled.png", new Box(178, 78, 24, 24, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/filter_enabled.png", new Box(180, 41, 26, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/filter_disabled.png", new Box(152, 41, 26, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/filter_enabled_highlighted.png", new Box(180, 59, 26, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/filter_disabled_highlighted.png", new Box(152, 59, 26, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/page_forward.png", new Box(1, 208, 12, 17, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/page_forward_highlighted.png", new Box(1, 226, 12, 17, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/page_backward.png", new Box(14, 208, 12, 17, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/page_backward_highlighted.png", new Box(14, 226, 12, 17, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/tab.png", new Box(153, 2, 35, 27, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/tab_selected.png", new Box(188, 2, 35, 27, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/slot_many_craftable.png", new Box(29, 231, 25, 25, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/slot_craftable.png", new Box(29, 206, 25, 25, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/slot_many_uncraftable.png", new Box(54, 231, 25, 25, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/slot_uncraftable.png", new Box(54, 206, 25, 25, 256, 256))),
                input("textures/gui/social_interactions.png",
                        new OutputFile("textures/gui/sprites/social_interactions/mute_button.png", new Box(0, 38, 20, 20, 256, 256)),
                        new OutputFile("textures/gui/sprites/social_interactions/mute_button_highlighted.png", new Box(0, 58, 20, 20, 256, 256)),
                        new OutputFile("textures/gui/sprites/social_interactions/unmute_button.png", new Box(20, 38, 20, 20, 256, 256)),
                        new OutputFile("textures/gui/sprites/social_interactions/unmute_button_highlighted.png", new Box(20, 58, 20, 20, 256, 256)),
                        (new OutputFile("textures/gui/sprites/social_interactions/background.png", new Box(1, 1, 236, 34, 256, 256))).metadata("{\n    \"gui\": {\n        \"scaling\": {\n            \"type\": \"nine_slice\",\n            \"width\": 236,\n            \"height\": 34,\n            \"border\": 8\n        }\n    }\n}\n"),
                        new OutputFile("textures/gui/sprites/icon/search.png", new Box(243, 1, 12, 12, 256, 256))),
                input("textures/gui/spectator_widgets.png",
                        new OutputFile("textures/gui/sprites/spectator/teleport_to_player.png", new Box(0, 0, 16, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/spectator/teleport_to_team.png", new Box(16, 0, 16, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/spectator/close.png", new Box(128, 0, 16, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/spectator/scroll_left.png", new Box(144, 0, 16, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/spectator/scroll_right.png", new Box(160, 0, 16, 16, 256, 256))),
                input("textures/gui/container/smoker.png",
                        new OutputFile("textures/gui/sprites/container/smoker/lit_progress.png", new Box(176, 0, 14, 14, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/smoker/burn_progress.png", new Box(176, 14, 24, 16, 256, 256))),
                input("textures/gui/recipe_button.png",
                        new OutputFile("textures/gui/sprites/recipe_book/button.png", new Box(0, 0, 20, 18, 256, 256)),
                        new OutputFile("textures/gui/sprites/recipe_book/button_highlighted.png", new Box(0, 19, 20, 18, 256, 256))),
                input("textures/gui/icons.png",
                        new OutputFile("textures/gui/sprites/icon/ping_unknown.png", new Box(0, 216, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/icon/ping_5.png", new Box(0, 176, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/icon/ping_4.png", new Box(0, 184, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/icon/ping_3.png", new Box(0, 192, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/icon/ping_2.png", new Box(0, 200, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/icon/ping_1.png", new Box(0, 208, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/container_blinking.png", new Box(25, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/container.png", new Box(16, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/full_blinking.png", new Box(70, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/half_blinking.png", new Box(79, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/absorbing_full_blinking.png", new Box(160, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/full.png", new Box(52, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/absorbing_half_blinking.png", new Box(169, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/half.png", new Box(61, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/crosshair.png", new Box(0, 0, 15, 15, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/crosshair_attack_indicator_full.png", new Box(68, 94, 16, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/crosshair_attack_indicator_background.png", new Box(36, 94, 16, 4, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/crosshair_attack_indicator_progress.png", new Box(52, 94, 16, 4, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/hotbar_attack_indicator_background.png", new Box(0, 94, 18, 18, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/hotbar_attack_indicator_progress.png", new Box(18, 94, 18, 18, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/jump_bar_background.png", new Box(0, 84, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/jump_bar_cooldown.png", new Box(0, 74, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/jump_bar_progress.png", new Box(0, 89, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/experience_bar_background.png", new Box(0, 64, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/experience_bar_progress.png", new Box(0, 69, 182, 5, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/armor_full.png", new Box(34, 9, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/armor_half.png", new Box(25, 9, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/armor_empty.png", new Box(16, 9, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/food_empty_hunger.png", new Box(133, 27, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/food_half_hunger.png", new Box(97, 27, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/food_full_hunger.png", new Box(88, 27, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/food_empty.png", new Box(16, 27, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/food_half.png", new Box(61, 27, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/food_full.png", new Box(52, 27, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/air.png", new Box(16, 18, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/air_bursting.png", new Box(25, 18, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/container_hardcore.png", new Box(16, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/container_hardcore_blinking.png", new Box(25, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/hardcore_full.png", new Box(52, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/hardcore_full_blinking.png", new Box(70, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/hardcore_half.png", new Box(61, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/hardcore_half_blinking.png", new Box(79, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/poisoned_full.png", new Box(88, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/poisoned_full_blinking.png", new Box(106, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/poisoned_half.png", new Box(97, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/poisoned_half_blinking.png", new Box(115, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/poisoned_hardcore_full.png", new Box(88, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/poisoned_hardcore_full_blinking.png", new Box(106, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/poisoned_hardcore_half.png", new Box(97, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/poisoned_hardcore_half_blinking.png", new Box(115, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/withered_full.png", new Box(124, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/withered_full_blinking.png", new Box(142, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/withered_half.png", new Box(133, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/withered_half_blinking.png", new Box(151, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/withered_hardcore_full.png", new Box(124, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/withered_hardcore_full_blinking.png", new Box(142, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/withered_hardcore_half.png", new Box(133, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/withered_hardcore_half_blinking.png", new Box(151, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/absorbing_full.png", new Box(160, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/absorbing_half.png", new Box(169, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/absorbing_hardcore_full.png", new Box(160, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/absorbing_hardcore_full_blinking.png", new Box(160, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/absorbing_hardcore_half.png", new Box(169, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/absorbing_hardcore_half_blinking.png", new Box(169, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/frozen_full.png", new Box(178, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/frozen_full_blinking.png", new Box(178, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/frozen_half.png", new Box(187, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/frozen_half_blinking.png", new Box(187, 0, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/frozen_hardcore_full.png", new Box(178, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/frozen_hardcore_full_blinking.png", new Box(178, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/frozen_hardcore_half.png", new Box(187, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/frozen_hardcore_half_blinking.png", new Box(187, 45, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/vehicle_container.png", new Box(52, 9, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/vehicle_full.png", new Box(88, 9, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/heart/vehicle_half.png", new Box(97, 9, 9, 9, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/incompatible.png", new Box(0, 216, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/unreachable.png", new Box(0, 216, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/ping_5.png", new Box(0, 176, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/ping_4.png", new Box(0, 184, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/ping_3.png", new Box(0, 192, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/ping_2.png", new Box(0, 200, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/ping_1.png", new Box(0, 208, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/pinging_1.png", new Box(10, 176, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/pinging_2.png", new Box(10, 184, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/pinging_3.png", new Box(10, 192, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/pinging_4.png", new Box(10, 200, 10, 8, 256, 256)),
                        new OutputFile("textures/gui/sprites/server_list/pinging_5.png", new Box(10, 208, 10, 8, 256, 256))),
                input("textures/gui/container/inventory.png",
                        new OutputFile("textures/gui/sprites/hud/effect_background_ambient.png", new Box(165, 166, 24, 24, 256, 256)),
                        new OutputFile("textures/gui/sprites/hud/effect_background.png", new Box(141, 166, 24, 24, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/inventory/effect_background_large.png", new Box(0, 166, 120, 32, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/inventory/effect_background_small.png", new Box(0, 198, 32, 32, 256, 256))),
                input("textures/gui/unseen_notification.png",
                        new OutputFile("textures/gui/sprites/icon/unseen_notification.png", new Box(0, 0, 10, 10, 10, 10))),
                input("textures/gui/container/loom.png",
                        new OutputFile("textures/gui/sprites/container/loom/banner_slot.png", new Box(176, 0, 16, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/loom/dye_slot.png", new Box(192, 0, 16, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/loom/pattern_slot.png", new Box(208, 0, 16, 16, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/loom/scroller.png", new Box(232, 0, 12, 15, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/loom/scroller_disabled.png", new Box(244, 0, 12, 15, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/loom/pattern_selected.png", new Box(0, 180, 14, 14, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/loom/pattern_highlighted.png", new Box(0, 194, 14, 14, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/loom/pattern.png", new Box(0, 166, 14, 14, 256, 256)),
                        (new OutputFile("textures/gui/sprites/container/loom/error.png", new Box(176, 17, 17, 16, 256, 256))).apply(spriteExtender())),
                input("assets/realms/textures/gui/realms/invite_icon.png",
                        new OutputFile("textures/gui/sprites/icon/invite.png", new Box(0, 0, 18, 15, 18, 30)),
                        new OutputFile("textures/gui/sprites/icon/invite_highlighted.png", new Box(0, 15, 18, 15, 18, 30))),
                input("textures/gui/container/cartography_table.png",
                        new OutputFile("textures/gui/sprites/container/cartography_table/error.png", new Box(226, 132, 28, 21, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/cartography_table/scaled_map.png", new Box(176, 66, 66, 66, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/cartography_table/duplicated_map.png", new Box(176, 132, 50, 66, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/cartography_table/map.png", new Box(176, 0, 66, 66, 256, 256)),
                        new OutputFile("textures/gui/sprites/container/cartography_table/locked.png", new Box(52, 214, 10, 14, 256, 256))),
                input("assets/realms/textures/gui/realms/cross_player_icon.png",
                        new OutputFile("textures/gui/sprites/player_list/remove_player.png", new Box(0, 0, 8, 7, 8, 14)),
                        new OutputFile("textures/gui/sprites/player_list/remove_player_highlighted.png", new Box(0, 7, 8, 7, 8, 14))),
                input("textures/gui/sprites/accessibility.png",
                        new OutputFile("textures/gui/sprites/icon/accessibility.png", new Box(0, 0, 15, 15, 15, 15))),
                input("textures/gui/container/stats_icons.png",
                        new OutputFile("textures/gui/sprites/container/slot.png", new Box(0, 0, 18, 18, 128, 128)),
                        new OutputFile("textures/gui/sprites/statistics/block_mined.png", new Box(54, 18, 18, 18, 128, 128)),
                        new OutputFile("textures/gui/sprites/statistics/item_broken.png", new Box(72, 18, 18, 18, 128, 128)),
                        new OutputFile("textures/gui/sprites/statistics/item_crafted.png", new Box(18, 18, 18, 18, 128, 128)),
                        new OutputFile("textures/gui/sprites/statistics/item_used.png", new Box(36, 18, 18, 18, 128, 128)),
                        new OutputFile("textures/gui/sprites/statistics/item_picked_up.png", new Box(90, 18, 18, 18, 128, 128)),
                        new OutputFile("textures/gui/sprites/statistics/item_dropped.png", new Box(108, 18, 18, 18, 128, 128)),
                        new OutputFile("textures/gui/sprites/statistics/header.png", new Box(0, 18, 18, 18, 128, 128)),
                        new OutputFile("textures/gui/sprites/statistics/sort_up.png", new Box(36, 0, 18, 18, 128, 128)),
                        new OutputFile("textures/gui/sprites/statistics/sort_down.png", new Box(18, 0, 18, 18, 128, 128))), moveRealmsToMinecraft("realms/adventure"), moveRealmsToMinecraft("realms/darken"), moveRealmsToMinecraft("realms/empty_frame"), moveRealmsToMinecraft("realms/experience"), moveRealmsToMinecraft("realms/inspiration"), moveRealmsToMinecraft("realms/new_world"), moveRealmsToMinecraft("realms/popup"), moveRealmsToMinecraft("realms/survival_spawn"), moveRealmsToMinecraft("realms/upload"), moveRealmsToMinecraft("title/realms"), copy("advancements/backgrounds/adventure"), copy("advancements/backgrounds/end"), copy("advancements/backgrounds/husbandry"), copy("advancements/backgrounds/nether"), copy("advancements/backgrounds/stone"), copy("advancements/backgrounds/window"), copy("advancements/window"), copy("container/creative_inventory/tab_inventory"), copy("container/creative_inventory/tab_item_search"), copy("container/creative_inventory/tab_items"), clip("container/anvil", STANDARD_CONTAINER_BOX), clip("container/beacon", new Box(0, 0, 230, 219, 256, 256)), clip("container/blast_furnace", STANDARD_CONTAINER_BOX), clip("container/brewing_stand", STANDARD_CONTAINER_BOX), clip("container/cartography_table", STANDARD_CONTAINER_BOX), clip("container/crafting_table", STANDARD_CONTAINER_BOX), clip("container/dispenser", STANDARD_CONTAINER_BOX), clip("container/enchanting_table", STANDARD_CONTAINER_BOX), clip("container/furnace", STANDARD_CONTAINER_BOX), clip("container/gamemode_switcher", new Box(0, 0, 125, 75, 128, 128)), copy("container/generic_54"), clip("container/grindstone", STANDARD_CONTAINER_BOX), clip("container/hopper", new Box(0, 0, 176, 133, 256, 256)), clip("container/horse", STANDARD_CONTAINER_BOX), clip("container/inventory", STANDARD_CONTAINER_BOX), clip("container/loom", STANDARD_CONTAINER_BOX), clip("container/shulker_box", STANDARD_CONTAINER_BOX), clip("container/smithing", STANDARD_CONTAINER_BOX), clip("container/smoker", STANDARD_CONTAINER_BOX), clip("container/stonecutter", STANDARD_CONTAINER_BOX), clip("container/villager2", "container/villager", new Box(0, 0, 276, 166, 512, 256)), copy("hanging_signs/acacia"), copy("hanging_signs/bamboo"), copy("hanging_signs/birch"), copy("hanging_signs/cherry"), copy("hanging_signs/crimson"), copy("hanging_signs/dark_oak"), copy("hanging_signs/jungle"), copy("hanging_signs/mangrove"), copy("hanging_signs/oak"), copy("hanging_signs/spruce"), copy("hanging_signs/warped"), copy("presets/isles"), copy("title/background/panorama_0"), copy("title/background/panorama_1"), copy("title/background/panorama_2"), copy("title/background/panorama_3"), copy("title/background/panorama_4"), copy("title/background/panorama_5"), copy("title/background/panorama_overlay"), copy("title/edition"), copy("title/minceraft"), copy("title/minecraft"), copy("title/mojangstudios"), clip("book", new Box(20, 1, 146, 180, 256, 256)), copy("demo_background"), copy("footer_separator"), copy("header_separator"), copy("light_dirt_background"), copy("options_background"), clip("recipe_book", new Box(0, 0, 149, 168, 256, 256)));

        OUTPUT_PATHS = INPUTS.stream().map(i -> i.outputs.stream().map(o -> o.path).collect(Collectors.toSet())).flatMap(s -> s.stream()).collect(Collectors.toSet());

    }

}
