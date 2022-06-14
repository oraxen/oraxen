package io.th0rgal.oraxen.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import io.th0rgal.oraxen.font.GlyphTransformation;
import io.th0rgal.oraxen.font.ShiftTransformation;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.transformation.TransformationRegistry;
import net.kyori.adventure.text.minimessage.transformation.TransformationType;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Utils {

    public static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    public static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .transformations(TransformationRegistry.builder()
                    .add(new TransformationType<>(
                            GlyphTransformation::canParse, new GlyphTransformation.Parser()
                    ))
                    .add(new TransformationType<>(
                            ShiftTransformation::canParse, new ShiftTransformation.Parser()
                    )).build()
            )
            .build();

    public static List<String> toLowercaseList(final String... values) {
        final ArrayList<String> list = new ArrayList<>();
        for (final String value : values)
            list.add(value.toLowerCase(Locale.ENGLISH));
        return list;
    }

    public static String[] toLowercase(final String... values) {
        for (int index = 0; index < values.length; index++)
            values[index] = values[index].toLowerCase();
        return values;
    }

    public static long getVersion(final String format) {
        return Long.parseLong(OffsetDateTime.now().format(DateTimeFormatter.ofPattern(format)));
    }

    public static String removeExtension(String s) {

        String separator = System.getProperty("file.separator");
        String filename;

        // Remove the path upto the filename.
        int lastSeparatorIndex = s.lastIndexOf(separator);
        if (lastSeparatorIndex == -1) filename = s;
        else filename = s.substring(lastSeparatorIndex + 1);

        // Remove the extension.
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex == -1)
            return filename;

        return filename.substring(0, extensionIndex);
    }

    public static void writeStringToFile(final File file, final String content) {
        try {
            file.getParentFile().mkdirs();
            final BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public static int firstEmpty(Map<String, Integer> map, int min) {
        while (map.containsValue(min))
            min++;
        return min;
    }

    public static void sendAnimation(Player player, EquipmentSlot hand) {
        final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        final PacketContainer animation = protocolManager.createPacket(PacketType.Play.Server.ANIMATION);
        animation.getIntegers().write(0, player.getEntityId());
        animation.getIntegers().write(1, (hand == EquipmentSlot.HAND) ? 0 : 3);
        try {
            protocolManager.sendServerPacket(player, animation);
        } catch (final InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
