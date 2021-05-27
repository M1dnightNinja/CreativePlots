package me.m1dnightninja.creativeplots.api;

import me.m1dnightninja.creativeplots.api.plot.IPlotWorld;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.config.FileConfig;
import me.m1dnightninja.midnightcore.api.module.lang.ILangModule;
import me.m1dnightninja.midnightcore.api.module.lang.ILangProvider;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;
import me.m1dnightninja.midnightcore.api.registry.MRegistry;

import java.io.File;

public class CreativePlotsAPI {

    private static CreativePlotsAPI instance;

    private final MRegistry<IPlotWorld> plotWorlds;
    private final ILangProvider langProvider;
    private final FileConfig configFile;
    private final ConfigSection configDefaults;

    public CreativePlotsAPI(File dataFolder, MidnightCoreAPI api, ConfigSection langDefaults, ConfigSection configDefaults) {

        instance = this;

        this.plotWorlds = new MRegistry<>();
        this.langProvider = api.getModule(ILangModule.class).createLangProvider(new File(dataFolder, "lang"), api.getDefaultConfigProvider(), langDefaults);
        this.configFile = new FileConfig(new File(dataFolder, "config" + api.getDefaultConfigProvider().getFileExtension()));
        this.configDefaults = configDefaults;

        loadConfig();

    }

    public long reload() {

        long time = System.currentTimeMillis();

        // Reset content
        plotWorlds.clear();

        // Load new content
        loadConfig();

        time = System.currentTimeMillis() - time;
        return time;

    }

    public ILangProvider getLangProvider() {
        return langProvider;
    }

    public void saveWorlds() {

        if(plotWorlds.getSize() == 0) return;

        ConfigSection worlds = configFile.getRoot().getOrCreateSection("worlds");
        for(IPlotWorld world : plotWorlds) {

            MIdentifier id = plotWorlds.getId(world);
            worlds.set(id.toString(), world);
        }

        configFile.save();
    }

    private void loadConfig() {

        ConfigSection root = configFile.getRoot();

        root.fill(configDefaults);
        configFile.save();

        if(root.has("worlds", ConfigSection.class)) {

            ConfigSection sec = root.getSection("worlds");
            for(String key : sec.getKeys()) {

                if(!sec.has(key, ConfigSection.class)) continue;
                MIdentifier id = MIdentifier.parse(key);

                plotWorlds.register(id, sec.get(key, IPlotWorld.class));
            }
        }

    }

    public IPlotWorld getPlotWorld(MPlayer player) {

        return plotWorlds.get(player.getDimension());

    }

    public static CreativePlotsAPI getInstance() {
        return instance;
    }
}
