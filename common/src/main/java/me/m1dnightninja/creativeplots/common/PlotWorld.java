package me.m1dnightninja.creativeplots.common;

import me.m1dnightninja.creativeplots.api.plot.IPlot;
import me.m1dnightninja.creativeplots.api.plot.IPlotRegistry;
import me.m1dnightninja.creativeplots.api.plot.IPlotWorld;
import me.m1dnightninja.creativeplots.api.plot.PlotPos;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.config.ConfigSerializer;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.math.Vec3i;
import me.m1dnightninja.midnightcore.api.player.MPlayer;

import java.util.ArrayList;
import java.util.List;

public class PlotWorld implements IPlotWorld {

    protected final int worldHeight;
    protected final int worldFloor;
    protected final int generationHeight;
    protected final int plotSize;
    protected final int roadSize;

    protected final Vec3d spawnLocation;
    protected final PlotRegistry plotRegistry;

    public PlotWorld(int plotSize, int roadSize, int genHeight, int worldFloor, int worldHeight) {

        this.plotSize = plotSize;
        this.roadSize = roadSize;
        this.generationHeight = genHeight;
        this.worldFloor = worldFloor;
        this.worldHeight = worldHeight;

        this.plotRegistry = new PlotRegistry();

        float pos = roadSize % 2 == 0 ? 0.5f : 0.0f;
        spawnLocation = new Vec3d(pos, genHeight, pos);

    }


    @Override
    public int getPlotSize() {

        return plotSize;
    }

    @Override
    public int getRoadSize() {

        return roadSize;
    }

    @Override
    public int getWorldFloor() {
        return worldFloor;
    }

    @Override
    public int getWorldHeight() {

        return worldHeight;
    }

    @Override
    public int getGenerationHeight() {

        return generationHeight;
    }

    @Override
    public Vec3d getSpawnLocation() {

        return spawnLocation;
    }

    @Override
    public Vec3i toLocation(PlotPos position) {

        int offset = ((int) Math.ceil(roadSize / 2.0f));

        int x = (plotSize * position.getX()) + (roadSize * position.getX()) + offset;
        int z = (plotSize * position.getZ()) + (roadSize * position.getZ()) + offset;

        return new Vec3i(x, worldHeight, z);
    }

    @Override
    public boolean canInteract(MPlayer pl, Vec3i block) {
        return true;
    }

    @Override
    public boolean canModify(MPlayer pl, Vec3i block) {

        if(block.getY() <= worldFloor) return false;

        PlotPos[] poss;

        PlotPos pos = PlotPos.fromCoords(block.getX(), block.getZ(), plotSize, roadSize);
        if(pos == null) {
            poss = PlotPos.surroundingFromCoords(block.getX(), block.getZ(), plotSize, roadSize);
        } else {
            poss = new PlotPos[] { pos };
        }

        for(PlotPos p : poss) {
            IPlot plot = plotRegistry.getPlotAt(p);

            if(plot == null) continue;

            if(plot.contains(block)) {
                if(plot.canEdit(pl)) return true;
            }
        }

        return pl.hasPermission("creativeplots.editanywhere");
    }

    @Override
    public IPlotRegistry getPlotRegistry() {
        return plotRegistry;
    }

    public static final ConfigSerializer<IPlotWorld> SERIALIZER = new ConfigSerializer<IPlotWorld>() {

        @Override
        public IPlotWorld deserialize(ConfigSection section) {

            int plotSize = section.getInt("plot_size");
            int roadSize = section.getInt("road_size");
            int worldHeight = section.getInt("world_height");
            int worldFloor = section.getInt("world_floor");
            int generationHeight = section.getInt("generation_height");

            PlotWorld out = new PlotWorld(plotSize, roadSize, generationHeight, worldFloor, worldHeight);

            if(section.has("plots", List.class)) {
                List<ConfigSection> lst = section.getList("plots", ConfigSection.class);
                for(ConfigSection sec : lst) {

                    Plot p = Plot.fromConfig(out, sec);
                    p.register(out.getPlotRegistry());
                }
            }

            return out;
        }

        @Override
        public ConfigSection serialize(IPlotWorld object) {

            ConfigSection out = new ConfigSection();
            out.set("plot_size", object.getPlotSize());
            out.set("road_size", object.getRoadSize());
            out.set("world_height", object.getWorldHeight());
            out.set("world_floor", object.getWorldFloor());
            out.set("generation_height", object.getGenerationHeight());

            List<ConfigSection> plots = new ArrayList<>();
            for(IPlot pl : object.getPlotRegistry()) {
                plots.add(pl.serialize());
            }
            out.set("plots", plots);

            return out;
        }
    };

}
