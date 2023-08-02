package io.th0rgal.oraxen.utils;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.UndeclaredThrowableException;

public interface CacheInvoker {

    MethodHandle cache(MethodHandle mh);

    static CacheInvoker get() {
        MethodHandle invoker = new CacheCallSite().dynamicInvoker();
        return mh -> {
            try {
                return (MethodHandle) invoker.invokeExact(mh);
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new UndeclaredThrowableException(e);
            }
        };
    }
}
