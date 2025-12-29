package com.simplerender;

import com.simplerender.app.EngineConfig;
import com.simplerender.app.GameApplication;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        EngineConfig config = EngineConfig.defaultConfig();
        GameApplication application = new GameApplication(config);
        application.run();
    }
}
