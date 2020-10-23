package io.th0rgal.oraxen.utils.reflection;

import static io.th0rgal.oraxen.utils.reflection.ReflectionProvider.ORAXEN;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;

import com.syntaxphoenix.syntaxapi.nbt.NbtCompound;
import com.syntaxphoenix.syntaxapi.nbt.NbtNamedTag;
import com.syntaxphoenix.syntaxapi.nbt.NbtTag;
import com.syntaxphoenix.syntaxapi.nbt.NbtType;
import com.syntaxphoenix.syntaxapi.nbt.tools.NbtDeserializer;
import com.syntaxphoenix.syntaxapi.nbt.tools.NbtSerializer;
import com.syntaxphoenix.syntaxapi.reflection.Reflect;
import com.syntaxphoenix.syntaxapi.utils.java.Exceptions;

import io.th0rgal.oraxen.utils.logs.Logs;

public class NbtTools {

    public static final NbtDeserializer DESERIALIZER = new NbtDeserializer(false);
    public static final NbtSerializer SERIALIZER = new NbtSerializer(false);

    public static Object toMinecraft(NbtCompound compound) {

        Optional<Reflect> option0 = ORAXEN.getOptionalReflect("nms_nbt_stream_tools");
        if (option0.isEmpty())
            throw new IllegalStateException("Oraxen Reflections aren't setup properly?");

        try {

            byte[] bytes = SERIALIZER.toBytes(new NbtNamedTag("root", compound));

            ByteArrayInputStream stream = new ByteArrayInputStream(bytes);

            Object minecraft = option0.get().run("read", new DataInputStream(stream));

            stream.close();

            return minecraft;

        } catch (IOException exception) {
            Logs.logError(Exceptions.stackTraceToString(exception));
        }
        return null;
    }

    public static NbtCompound fromMinecraft(Object compound) {

        Optional<Reflect> option0 = ORAXEN.getOptionalReflect("nms_nbt_stream_tools");
        if (!option0.isPresent())
            throw new IllegalStateException("Oraxen Reflections aren't setup properly?");

        try {

            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            option0.get().run("write", compound, new DataOutputStream(stream));

            NbtTag tag = DESERIALIZER.fromStream(new ByteArrayInputStream(stream.toByteArray())).getTag();

            stream.close();

            return tag.getType() == NbtType.COMPOUND ? (NbtCompound) tag : new NbtCompound();

        } catch (IOException exception) {
            Logs.logError(Exceptions.stackTraceToString(exception));
        }
        return null;
    }

}
