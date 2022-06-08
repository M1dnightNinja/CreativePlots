package org.wallentines.creativeplots.common;

import org.wallentines.creativeplots.api.CreativePlotsAPI;
import org.wallentines.creativeplots.api.plot.IPlot;
import org.wallentines.creativeplots.api.plot.IPlotRegistry;
import org.wallentines.creativeplots.api.plot.IPlotWorld;
import org.wallentines.creativeplots.api.plot.PlotPos;
import org.wallentines.midnightcore.api.player.Location;
import org.wallentines.midnightlib.config.ConfigSection;
import org.wallentines.midnightlib.config.serialization.ConfigSerializer;
import org.wallentines.midnightlib.math.Vec3d;
import org.wallentines.midnightlib.math.Vec3i;
import org.wallentines.midnightcore.api.module.lang.LangModule;
import org.wallentines.midnightcore.api.module.lang.PlaceholderSupplier;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlotWorld implements IPlotWorld {

    protected final int worldHeight;
    protected final int worldFloor;
    protected final int generationHeight;
    protected final int plotSize;
    protected final int roadSize;

    protected final Identifier borderBlock;
    protected final Identifier borderBlockClaimed;

    protected final Vec3d spawnLocation;
    protected final PlotRegistry plotRegistry;

    protected final HashMap<MPlayer, Vec3d> locations = new HashMap<>();

    public PlotWorld(int plotSize, int roadSize, int genHeight, int worldFloor, int worldHeight, Identifier borderBlock, Identifier borderBlockClaimed) {

        this.plotSize = plotSize;
        this.roadSize = roadSize;
        this.generationHeight = genHeight;
        this.worldFloor = worldFloor;
        this.worldHeight = worldHeight;

        this.borderBlock = borderBlock;
        this.borderBlockClaimed = borderBlockClaimed;

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
    public Identifier getBorderBlock(PlotPos position) {
        return plotRegistry.getPlotAt(position) == null ? borderBlock : borderBlockClaimed;
    }

    @Override
    public Identifier getUnclaimedBorderBlock() {
        return borderBlock;
    }

    @Override
    public Identifier getClaimedBorderBlock() {
        return borderBlockClaimed;
    }

    @Override
    public Vec3d getSpawnLocation() {

        return spawnLocation;
    }

    @Override
    public Vec3i toLocation(PlotPos position) {

        int offset = roadSize / 2;
        if(roadSize % 2 == 1) offset += 1;

        int totalSize = plotSize + roadSize;

        int x = totalSize * position.getX() + offset;
        int z = totalSize * position.getZ() + offset;

        return new Vec3i(x, generationHeight + 1, z);
    }

    @Override
    public boolean canInteract(MPlayer pl, Vec3i block) {

        IPlot plot = getPlot(block);
        if(plot != null) {
            return !plot.isDenied(pl.getUUID());
        }

        return true;
    }

    @Override
    public boolean canModify(MPlayer pl, Vec3i block) {

        IPlot plot = getPlot(block);
        if(plot != null) {
            if(plot.canEdit(pl)) return true;
        }

        return pl.hasPermission("creativeplots.editanywhere", 4);
    }

    @Override
    public IPlotRegistry getPlotRegistry() {
        return plotRegistry;
    }

    @Override
    public void onEnteredWorld(MPlayer player) {
        locations.put(player, player.getLocation().getCoordinates());
    }

    @Override
    public void onLeftWorld(MPlayer player) {
        locations.remove(player);
    }

    @Override
    public void onTick() {
        for(Map.Entry<MPlayer, Vec3d> ent : locations.entrySet()) {
            Vec3d ploc = ent.getKey().getLocation().getCoordinates();
            Vec3d oloc = ent.getValue();
            if(!ploc.equals(ent.getValue())) {

                locations.put(ent.getKey(), ploc);
                playerMoved(ent.getKey(), oloc, ploc);
            }
        }
    }

    @Override
    public IPlot getPlot(Vec3i block) {

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

                return plot;
            }
        }

        return null;
    }

    private void playerMoved(MPlayer pl, Vec3d oldLoc, Vec3d newLoc) {

        IPlot oldPlot = getPlot(oldLoc.truncate());
        IPlot newPlot = getPlot(newLoc.truncate());
        if(newPlot != null && oldPlot != newPlot) {

            if(newPlot.isDenied(pl.getUUID())) {
                locations.put(pl, oldLoc);
                pl.teleport(new Location(pl.getLocation().getWorldId(), oldLoc, pl.getLocation().getYaw(), pl.getLocation().getPitch()));
                oldPlot.onLeave(pl);

            } else {

                newPlot.onEnter(pl);
            }
        }

    }


    public static final ConfigSerializer<PlotWorld> SERIALIZER = new ConfigSerializer<>() {

        @Override
        public PlotWorld deserialize(ConfigSection section) {

            int plotSize = section.getInt("plot_size");
            int roadSize = section.getInt("road_size");
            int worldHeight = section.getInt("world_height");
            int worldFloor = section.getInt("world_floor");
            int generationHeight = section.getInt("generation_height");

            Identifier borderBlock = section.get("border_block", Identifier.class);
            Identifier borderBlockClaimed = section.get("border_block_claimed", Identifier.class);

            PlotWorld out = new PlotWorld(plotSize, roadSize, generationHeight, worldFloor, worldHeight, borderBlock, borderBlockClaimed);

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
        public ConfigSection serialize(PlotWorld object) {

            ConfigSection out = new ConfigSection();
            out.set("plot_size", object.getPlotSize());
            out.set("road_size", object.getRoadSize());
            out.set("world_height", object.getWorldHeight());
            out.set("world_floor", object.getWorldFloor());
            out.set("generation_height", object.getGenerationHeight());
            out.set("border_block", object.getUnclaimedBorderBlock());
            out.set("border_block_claimed", object.getClaimedBorderBlock());

            List<ConfigSection> plots = new ArrayList<>();
            for(IPlot pl : object.getPlotRegistry()) {
                plots.add(pl.serialize());
            }
            out.set("plots", plots);

            return out;
        }
    };

    public static void registerPlaceholders(LangModule mod) {

        mod.registerInlinePlaceholder("creativeplots_plotworld_id",   PlaceholderSupplier.create(IPlotWorld.class, pw -> CreativePlotsAPI.getInstance().getPlotWorldId(pw).toString()));
        mod.registerInlinePlaceholder("creativeplots_plotworld_name", PlaceholderSupplier.create(IPlotWorld.class, pw -> CreativePlotsAPI.getInstance().getPlotWorldId(pw).getPath()));

    }

}
