package mod.azuredoom.bettercrawling.platform;

import mod.azuredoom.bettercrawling.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public class Services {

    private Services() {
    }

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static <T> T load(Class<T> clazz) {

        return ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
    }
}
