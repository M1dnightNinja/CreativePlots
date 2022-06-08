package org.wallentines.creativeplots.fabric;

import net.fabricmc.api.ModInitializer;
import org.wallentines.creativeplots.api.CreativePlotsAPI;
import org.wallentines.creativeplots.api.plot.IPlotWorld;
import org.wallentines.creativeplots.common.Plot;
import org.wallentines.creativeplots.common.PlotWorld;
import org.wallentines.creativeplots.fabric.generator.PlotworldGenerator;
import org.wallentines.midnightcore.common.util.FileUtil;
import org.wallentines.midnightcore.fabric.event.MidnightCoreAPICreatedEvent;
import org.wallentines.midnightcore.fabric.event.server.CommandLoadEvent;
import org.wallentines.midnightcore.fabric.event.server.ServerStopEvent;
import org.wallentines.midnightlib.config.ConfigRegistry;
import org.wallentines.midnightlib.config.ConfigSection;
import org.wallentines.midnightcore.api.module.lang.LangModule;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.wallentines.midnightlib.config.serialization.json.JsonConfigProvider;
import org.wallentines.midnightlib.event.Event;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CreativePlots implements ModInitializer {

    @Override
    public void onInitialize() {

        Path dataFolder = Paths.get("config/CreativePlots");
        if (FileUtil.tryCreateDirectory(dataFolder) == null) {
            throw new IllegalStateException("Unable to create data directory " + dataFolder);
        }

        ConfigRegistry.INSTANCE.registerSerializer(PlotWorld.class, PlotWorld.SERIALIZER);

        new PlotListener().register();

        ConfigSection langDefaults = JsonConfigProvider.INSTANCE.loadFromStream(getClass().getResourceAsStream("/creativeplots/lang/en_us.json"));
        ConfigSection configDefaults = JsonConfigProvider.INSTANCE.loadFromStream(getClass().getResourceAsStream("/creativeplots/config.json"));

        Registry.register(Registry.CHUNK_GENERATOR, new ResourceLocation("creativeplots", "plotworld"), PlotworldGenerator.CODEC);

        Event.register(MidnightCoreAPICreatedEvent.class, this, event -> {

            CreativePlotsAPI capi = new CreativePlotsAPI(dataFolder, event.getAPI(), langDefaults, configDefaults);

            LangModule mod = event.getAPI().getModuleManager().getModule(LangModule.class);
            PlotWorld.registerPlaceholders(mod);
            Plot.registerPlaceholders(mod);

        });

        Event.register(CommandLoadEvent.class, this, event -> {

            new MainCommand().register(event.getDispatcher());
        });

        Event.register(ServerStopEvent.class, this, event -> {

            CreativePlotsAPI.getInstance().saveWorlds();
        });

    }
}
