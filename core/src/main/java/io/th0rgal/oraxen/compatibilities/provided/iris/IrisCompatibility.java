package io.th0rgal.oraxen.compatibilities.provided.iris;

import com.volmit.iris.Iris;
import com.volmit.iris.core.service.ExternalDataSVC;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.utils.logs.Logs;

public class IrisCompatibility extends CompatibilityProvider<Iris> {

    private OraxenDataProvider dataProvider;
    private ExternalDataSVC externalDataSVC;

    @Override
    public void enable(String pluginName) {
        super.enable(pluginName);

        try {
            externalDataSVC = Iris.service(ExternalDataSVC.class);
            if (externalDataSVC != null) {
                dataProvider = new OraxenDataProvider();
                externalDataSVC.registerProvider(dataProvider);
                Logs.logSuccess("Registered Oraxen data provider with Iris");
            } else {
                Logs.logWarning("Failed to get ExternalDataSVC from Iris");
            }
        } catch (Exception e) {
            Logs.logWarning("Failed to register Oraxen data provider with Iris: " + e.getMessage());
            if (io.th0rgal.oraxen.config.Settings.DEBUG.toBool()) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void disable() {
        super.disable();
        if (dataProvider != null && externalDataSVC != null) {
            try {
                boolean unregistered = false;
                for (String methodName : new String[]{"unregisterProvider", "deregisterProvider", "removeProvider"}) {
                    try {
                        java.lang.reflect.Method[] methods = externalDataSVC.getClass().getMethods();
                        for (java.lang.reflect.Method method : methods) {
                            if (!method.getName().equals(methodName)) continue;
                            if (method.getParameterCount() != 1) continue;
                            Class<?> paramType = method.getParameterTypes()[0];
                            if (!paramType.isAssignableFrom(dataProvider.getClass())) continue;
                            method.invoke(externalDataSVC, dataProvider);
                            unregistered = true;
                            break;
                        }

                        if (unregistered) break;
                    } catch (Exception ignored) {
                    }

                    if (unregistered) break;
                }

                if (unregistered) {
                    Logs.logSuccess("Unregistered Oraxen data provider from Iris");
                } else {
                    Logs.logWarning("Failed to unregister Oraxen data provider from Iris (no supported API method found)");
                }
            } catch (Exception e) {
                Logs.logWarning("Failed to unregister Oraxen data provider from Iris: " + e.getMessage());
                if (io.th0rgal.oraxen.config.Settings.DEBUG.toBool()) {
                    e.printStackTrace();
                }
            } finally {
                dataProvider = null;
                externalDataSVC = null;
            }
        }
    }
}
