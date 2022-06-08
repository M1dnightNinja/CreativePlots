package org.wallentines.creativeplots.api;

import org.wallentines.creativeplots.api.plot.IPlotWorld;
import org.wallentines.midnightcore.api.MidnightCoreAPI;
import org.wallentines.midnightlib.config.ConfigRegistry;
import org.wallentines.midnightlib.config.ConfigSection;
import org.wallentines.midnightlib.config.FileConfig;
import org.wallentines.midnightcore.api.module.lang.LangModule;
import org.wallentines.midnightcore.api.module.lang.LangProvider;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CreativePlotsAPI {

    private static CreativePlotsAPI instance;

    private final Registry<IPlotWorld> plotWorlds;
    private final LangProvider langProvider;
    private final FileConfig configFile;
    private final ConfigSection configDefaults;

    private final List<Identifier> allowedItems;

    private int maxSize = 4;

    public CreativePlotsAPI(Path dataFolder, MidnightCoreAPI api, ConfigSection langDefaults, ConfigSection configDefaults) {

        instance = this;

        this.plotWorlds = new Registry<>();
        this.langProvider = api.getModuleManager().getModule(LangModule.class).createProvider(dataFolder.resolve("lang"), langDefaults);
        this.configFile = FileConfig.findOrCreate("config", dataFolder.toFile());
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

    public LangProvider getLangProvider() {
        return langProvider;
    }

    public void saveWorlds() {

        if(plotWorlds.getSize() == 0) return;

        ConfigSection worlds = configFile.getRoot().getOrCreateSection("worlds");
        for(IPlotWorld world : plotWorlds) {

            Identifier id = plotWorlds.getId(world);
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
                Identifier id = Identifier.parse(key);

                plotWorlds.register(id, sec.get(key, IPlotWorld.class));
            }
        }

        allowedItems.addAll(root.getListFiltered("allowed_items", Identifier.class));
        maxSize = root.getInt("max_plot_size");

    }

    public int getMaxPlotSize() {
        return maxSize;
    }

    public IPlotWorld getPlotWorld(MPlayer player) {

        return plotWorlds.get(player.getLocation().getWorldId());
    }

    public IPlotWorld getPlotWorld(Identifier id) {

        return plotWorlds.get(id);

    }

    public Identifier getPlotWorldId(IPlotWorld world) {

        return plotWorlds.getId(world);
    }

    public Iterable<IPlotWorld> getWorlds() {
        return plotWorlds;
    }

    public boolean isAllowedItem(Identifier id) {
        for(Identifier item : allowedItems) {
            if(item.equals(id)) return true;
        }
        return false;
    }

    public static CreativePlotsAPI getInstance() {
        return instance;
    }
}
