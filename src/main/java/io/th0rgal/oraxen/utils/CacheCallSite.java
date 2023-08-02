package io.th0rgal.oraxen.utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;

public class CacheCallSite extends MutableCallSite {

    private static final MethodHandle FALLBACK_MH;
    private static final MethodHandle IDENTITY_CHECK_MH;

    static {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            FALLBACK_MH =
                    lookup.findVirtual(
                            CacheCallSite.class, "fallback",
                            MethodType.methodType(MethodHandle.class, MethodHandle.class)
                    );

            IDENTITY_CHECK_MH =
                    lookup.findStatic(
                            CacheCallSite.class, "identityCheck",
                            MethodType.methodType(boolean.class, MethodHandle.class, MethodHandle.class)
                    );
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public CacheCallSite() {
        super(MethodType.methodType(MethodHandle.class, MethodHandle.class));
        setTarget(FALLBACK_MH.bindTo(this));
    }

    private MethodHandle fallback(MethodHandle mh) {
        setTarget(
                MethodHandles.guardWithTest(
                        IDENTITY_CHECK_MH.bindTo(mh),
                        MethodHandles.dropArguments(
                                MethodHandles.constant(MethodHandle.class, mh),
                                0, MethodHandle.class
                        ),
                        new CacheCallSite().dynamicInvoker()
                )
        );

        return mh;
    }

    private static boolean identityCheck(MethodHandle mh1, MethodHandle mh2) {
        return mh1 == mh2;
    }
}
