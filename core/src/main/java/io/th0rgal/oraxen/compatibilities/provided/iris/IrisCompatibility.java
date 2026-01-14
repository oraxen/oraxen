package io.th0rgal.oraxen.compatibilities.provided.iris;

import com.volmit.iris.Iris;
import com.volmit.iris.core.service.ExternalDataSVC;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import io.th0rgal.oraxen.utils.logs.Logs;

public class IrisCompatibility extends CompatibilityProvider<Iris> {

    private OraxenDataProvider dataProvider;

    @Override
    public void enable(String pluginName) {
        super.enable(pluginName);

        try {
            ExternalDataSVC externalDataSVC = Iris.service(ExternalDataSVC.class);
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
        dataProvider = null;
    }
}
