package org.playuniverse.snowypine.bukkit.persistence;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

public class SnowyDataType<T, V> implements PersistentDataType<T, V> {

	public static final SnowyDataType<Byte, Boolean> BOOLEAN = of(Byte.class, Boolean.class, number -> number > 0, state -> (byte) (state ? 0 : 1));

	public static final SnowyDataType<byte[], UUID> UNIQUE_ID = of(byte[].class, UUID.class, array -> {
		ByteBuffer buffer = ByteBuffer.wrap(array);
		long mostSignificant = buffer.getLong();
		long leastSignificant = buffer.getLong();
		return new UUID(mostSignificant, leastSignificant);
	}, uniqueId -> {
		ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
		buffer.putLong(uniqueId.getMostSignificantBits());
		buffer.putLong(uniqueId.getLeastSignificantBits());
		return buffer.array();
	});

	/*
	 * 
	 */

	private final Class<T> primitiveType;
	private final Class<V> complexType;

	private final BiFunction<T, PersistentDataAdapterContext, V> fromPrimitive;
	private final BiFunction<V, PersistentDataAdapterContext, T> toPrimitive;

	SnowyDataType(Class<T> primitiveType, Class<V> complexType, BiFunction<T, PersistentDataAdapterContext, V> fromPrimitive,
		BiFunction<V, PersistentDataAdapterContext, T> toPrimitive) {
		this.primitiveType = primitiveType;
		this.complexType = complexType;
		this.fromPrimitive = fromPrimitive;
		this.toPrimitive = toPrimitive;
	}

	public Class<T> getPrimitiveType() {
		return this.primitiveType;
	}

	public Class<V> getComplexType() {
		return this.complexType;
	}

	public T toPrimitive(V complex, PersistentDataAdapterContext context) {
		return toPrimitive.apply(complex, context);
	}

	public V fromPrimitive(T primitive, PersistentDataAdapterContext context) {
		return fromPrimitive.apply(primitive, context);
	}

	public static <T> SnowyDataType<T, T> of(Class<T> type) {
		return new SnowyDataType<>(type, type, (value, context) -> value, (value, context) -> value);
	}

	public static <T, V> SnowyDataType<T, V> of(Class<T> primitive, Class<V> complex, Function<T, V> from, Function<V, T> to) {
		return new SnowyDataType<>(primitive, complex, (value, context) -> from.apply(value), (value, context) -> to.apply(value));
	}

	public static <T, V> SnowyDataType<T, V> of(Class<T> primitive, Class<V> complex, BiFunction<T, PersistentDataAdapterContext, V> from,
		BiFunction<V, PersistentDataAdapterContext, T> to) {
		return new SnowyDataType<>(primitive, complex, from, to);
	}

}