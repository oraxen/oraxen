package io.th0rgal.oraxen.utils.reflection;

import static io.th0rgal.oraxen.utils.reflection.ReflectionProvider.ORAXEN;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
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
        if (!option0.isPresent())
            throw new IllegalStateException("Oraxen Reflections aren't setup properly?");

        try {

            PipedInputStream input = new PipedInputStream();
            PipedOutputStream output = new PipedOutputStream(input);

            SERIALIZER.toStream(new NbtNamedTag("root", compound), output);
            Object minecraft = option0.get().run("read", new DataInputStream(input));

            input.close();
            output.close();

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

            PipedInputStream input = new PipedInputStream();
            PipedOutputStream output = new PipedOutputStream(input);

            option0.get().run("write", compound, new DataOutputStream(output));
            NbtTag tag = DESERIALIZER.fromStream(input).getTag();

            input.close();
            output.close();

            return tag.getType() == NbtType.COMPOUND ? (NbtCompound) tag : new NbtCompound();

        } catch (IOException exception) {
            Logs.logError(Exceptions.stackTraceToString(exception));
        }
        return null;
    }

}
