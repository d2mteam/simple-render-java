package com.simplerender.app;

import org.pf4j.CompoundPluginDescriptorFinder;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PropertiesPluginDescriptorFinder;
import org.pf4j.RuntimeMode;

import java.nio.file.Path;

public final class SimpleRenderPluginManager extends DefaultPluginManager {
    public SimpleRenderPluginManager(Path pluginsRoot) {
        super(pluginsRoot);
    }

    @Override
    public RuntimeMode getRuntimeMode() {
        return RuntimeMode.DEVELOPMENT;
    }

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        return new CompoundPluginDescriptorFinder()
            .add(new PropertiesPluginDescriptorFinder());
    }
}
