package me.m1dnightninja.creativeplots.fabric;

import me.m1dnightninja.creativeplots.api.CreativePlotsAPI;
import me.m1dnightninja.creativeplots.api.plot.IPlotWorld;
import me.m1dnightninja.creativeplots.common.Plot;
import me.m1dnightninja.creativeplots.common.PlotWorld;
import me.m1dnightninja.creativeplots.fabric.generator.PlotworldGenerator;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.module.lang.ILangModule;
import me.m1dnightninja.midnightcore.common.config.JsonConfigProvider;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.MidnightCoreModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
public class CreativePlots implements MidnightCoreModInitializer {

    @Override
    public void onInitialize() {

        new PlotListener().register();

    }

    @Override
    public void onAPICreated(MidnightCore core, MidnightCoreAPI api) {

        File dataFolder = new File("config/CreativePlots");

        api.getConfigRegistry().registerSerializer(IPlotWorld.class, PlotWorld.SERIALIZER);

        ConfigSection langDefaults = JsonConfigProvider.INSTANCE.loadFromStream(getClass().getResourceAsStream("/assets/creativeplots/lang/en_us.json"));
        ConfigSection configDefaults = JsonConfigProvider.INSTANCE.loadFromStream(getClass().getResourceAsStream("/assets/creativeplots/config.json"));

        CreativePlotsAPI capi = new CreativePlotsAPI(dataFolder, api, langDefaults, configDefaults);

        Registry.register(Registry.CHUNK_GENERATOR, new ResourceLocation("creativeplots", "plotworld"), PlotworldGenerator.CODEC);

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> capi.saveWorlds());


        /*for(int x = -100 ; x < 100 ; x++) {
            StringBuilder line = new StringBuilder(100);
            PlotPos cachedPos = null;
            for(int z = -100 ; z < 100 ; z++) {

                PlotPos pos = PlotPos.fromCoords(x,z,25,3);

                if(pos == null) {
                    line.append("#");
                } else {
                    if(!pos.equals(cachedPos)) {
                        line.append("(").append(pos.getX()).append(",").append(pos.getZ()).append(")");
                        z += 7;
                        cachedPos = pos;
                    } else {
                        line.append(" ");
                    }
                }
            }
            System.out.println(line);
        }*/


        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            new MainCommand().register(dispatcher);
        });

        ILangModule mod = api.getModule(ILangModule.class);
        PlotWorld.registerPlaceholders(mod);
        Plot.registerPlaceholders(mod);

    }


}
