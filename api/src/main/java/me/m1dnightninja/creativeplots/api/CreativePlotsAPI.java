package me.m1dnightninja.creativeplots.api;

import me.m1dnightninja.creativeplots.api.plot.IPlotWorld;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigRegistry;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.config.FileConfig;
import me.m1dnightninja.midnightcore.api.module.lang.ILangModule;
import me.m1dnightninja.midnightcore.api.module.lang.ILangProvider;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;
import me.m1dnightninja.midnightcore.api.registry.MRegistry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CreativePlotsAPI {

    private static CreativePlotsAPI instance;

    private final MRegistry<IPlotWorld> plotWorlds;
    private final ILangProvider langProvider;
    private final FileConfig configFile;
    private final ConfigSection configDefaults;

    private final List<MIdentifier> allowedItems;

    private int maxSize = 4;

    public CreativePlotsAPI(File dataFolder, MidnightCoreAPI api, ConfigSection langDefaults, ConfigSection configDefaults) {

        instance = this;

        this.plotWorlds = new MRegistry<>();
        this.langProvider = api.getModule(ILangModule.class).createLangProvider(new File(dataFolder, "lang"), langDefaults);
        this.configFile = new FileConfig(new File(dataFolder, "config" + ConfigRegistry.INSTANCE.getDefaultProvider().getFileExtension()));
        this.configDefaults = configDefaults;
        this.allowedItems = new ArrayList<>();

        loadConfig();

    }

    public long reload() {

        long time = System.currentTimeMillis();

        // Reset content
        plotWorlds.clear();
        allowedItems.clear();

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

        allowedItems.addAll(root.getListFiltered("allowed_items", MIdentifier.class));
        maxSize = root.getInt("max_plot_size");

    }

    public int getMaxPlotSize() {
        return maxSize;
    }

    public IPlotWorld getPlotWorld(MPlayer player) {

        return plotWorlds.get(player.getDimension());

    }

    public IPlotWorld getPlotWorld(MIdentifier id) {

        return plotWorlds.get(id);

    }

    public MIdentifier getPlotWorldId(IPlotWorld world) {

        return plotWorlds.getId(world);
    }

    public Iterable<IPlotWorld> getWorlds() {
        return plotWorlds;
    }

    public boolean isAllowedItem(MIdentifier id) {
        for(MIdentifier item : allowedItems) {
            if(item.equals(id)) return true;
        }
        return false;
    }

    public static CreativePlotsAPI getInstance() {
        return instance;
    }
}
