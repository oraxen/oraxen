//package org.playuniverse.snowypine.syntax.data;
//
//import java.util.function.BiFunction;
//import java.util.function.Function;
//
//import org.bukkit.Bukkit;
//import org.bukkit.Location;
//
//public class SnowDataType<P, C> implements DataType<P, C> {
//
//	public static final SnowDataType<IDataContainer, Location> LOCATION = of(IDataContainer.class, Location.class, (container, context) -> {
//		double x = container.get("x", PrimitiveDataType.DOUBLE);
//		double y = container.get("y", PrimitiveDataType.DOUBLE);
//		double z = container.get("z", PrimitiveDataType.DOUBLE);
//		float yaw = container.get("yaw", PrimitiveDataType.FLOAT);
//		float pitch = container.get("pitch", PrimitiveDataType.FLOAT);
//		if (container.has("world")) {
//			return new Location(Bukkit.getWorld(container.get("world", PrimitiveDataType.STRING)), x, y, z, yaw, pitch);
//		}
//		return new Location(null, x, y, z, yaw, pitch);
//	}, (location, context) -> {
//		IDataContainer container = context.newDataContainer();
//		container.set("x", location.getX(), PrimitiveDataType.DOUBLE);
//		container.set("y", location.getY(), PrimitiveDataType.DOUBLE);
//		container.set("z", location.getZ(), PrimitiveDataType.DOUBLE);
//		container.set("yaw", location.getPitch(), PrimitiveDataType.FLOAT);
//		container.set("pitch", location.getYaw(), PrimitiveDataType.FLOAT);
//		if (location.getWorld() != null) {
//			container.set("world", location.getWorld().getName(), PrimitiveDataType.STRING);
//		}
//		return container;
//	});
//
//	/*
//	 * 
//	 */
//
//	private final Class<P> primitiveType;
//	private final Class<C> complexType;
//
//	private final BiFunction<P, DataAdapterContext, C> fromPrimitive;
//	private final BiFunction<C, DataAdapterContext, P> toPrimitive;
//
//	SnowDataType(Class<P> primitiveType, Class<C> complexType, BiFunction<P, DataAdapterContext, C> fromPrimitive,
//		BiFunction<C, DataAdapterContext, P> toPrimitive) {
//		this.primitiveType = primitiveType;
//		this.complexType = complexType;
//		this.fromPrimitive = fromPrimitive;
//		this.toPrimitive = toPrimitive;
//	}
//
//	@Override
//	public Class<C> getComplex() {
//		return this.complexType;
//	}
//
//	@Override
//	public Class<P> getPrimitive() {
//		return this.primitiveType;
//	}
//
//	@Override
//	public P toPrimitive(DataAdapterContext context, C complex) {
//		return toPrimitive.apply(complex, context);
//	}
//
//	@Override
//	public C fromPrimitive(DataAdapterContext context, P primitive) {
//		return fromPrimitive.apply(primitive, context);
//	}
//
//	public static <P> SnowDataType<P, P> of(Class<P> type) {
//		return new SnowDataType<>(type, type, (value, context) -> value, (value, context) -> value);
//	}
//
//	public static <P, C> SnowDataType<P, C> of(Class<P> primitive, Class<C> complex, Function<P, C> from, Function<C, P> to) {
//		return new SnowDataType<>(primitive, complex, (value, context) -> from.apply(value), (value, context) -> to.apply(value));
//	}
//
//	public static <P, C> SnowDataType<P, C> of(Class<P> primitive, Class<C> complex, BiFunction<P, DataAdapterContext, C> from,
//		BiFunction<C, DataAdapterContext, P> to) {
//		return new SnowDataType<>(primitive, complex, from, to);
//	}
//
//}
