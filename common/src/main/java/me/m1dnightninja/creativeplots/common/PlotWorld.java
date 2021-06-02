package me.m1dnightninja.creativeplots.common;

import me.m1dnightninja.creativeplots.api.CreativePlotsAPI;
import me.m1dnightninja.creativeplots.api.plot.IPlot;
import me.m1dnightninja.creativeplots.api.plot.IPlotRegistry;
import me.m1dnightninja.creativeplots.api.plot.IPlotWorld;
import me.m1dnightninja.creativeplots.api.plot.PlotPos;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.config.ConfigSerializer;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.math.Vec3i;
import me.m1dnightninja.midnightcore.api.module.lang.ILangModule;
import me.m1dnightninja.midnightcore.api.module.lang.PlaceholderSupplier;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;

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

    protected final MIdentifier borderBlock;
    protected final MIdentifier borderBlockClaimed;

    protected final Vec3d spawnLocation;
    protected final PlotRegistry plotRegistry;

    protected final HashMap<MPlayer, Vec3d> locations = new HashMap<>();

    public PlotWorld(int plotSize, int roadSize, int genHeight, int worldFloor, int worldHeight, MIdentifier borderBlock, MIdentifier borderBlockClaimed) {

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
    public MIdentifier getBorderBlock(PlotPos position) {
        return plotRegistry.getPlotAt(position) == null ? borderBlock : borderBlockClaimed;
    }

    @Override
    public MIdentifier getUnclaimedBorderBlock() {
        return borderBlock;
    }

    @Override
    public MIdentifier getClaimedBorderBlock() {
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

        return pl.hasPermission("creativeplots.editanywhere");
    }

    @Override
    public IPlotRegistry getPlotRegistry() {
        return plotRegistry;
    }

    @Override
    public void onEnteredWorld(MPlayer player) {
        locations.put(player, player.getLocation());
    }

    @Override
    public void onLeftWorld(MPlayer player) {
        locations.remove(player);
    }

    @Override
    public void onTick() {
        for(Map.Entry<MPlayer, Vec3d> ent : locations.entrySet()) {
            if(!ent.getKey().getLocation().equals(ent.getValue())) {

                Vec3d loc = ent.getValue();
                locations.put(ent.getKey(), ent.getKey().getLocation());
                playerMoved(ent.getKey(), loc, ent.getKey().getLocation());
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
                pl.teleport(oldLoc, pl.getYaw(), pl.getPitch());

            } else {

                newPlot.sendEnterTitle(pl);
            }
        }

    }


    public static final ConfigSerializer<IPlotWorld> SERIALIZER = new ConfigSerializer<IPlotWorld>() {

        @Override
        public IPlotWorld deserialize(ConfigSection section) {

            int plotSize = section.getInt("plot_size");
            int roadSize = section.getInt("road_size");
            int worldHeight = section.getInt("world_height");
            int worldFloor = section.getInt("world_floor");
            int generationHeight = section.getInt("generation_height");

            MIdentifier borderBlock = section.get("border_block", MIdentifier.class);
            MIdentifier borderBlockClaimed = section.get("border_block_claimed", MIdentifier.class);

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
        public ConfigSection serialize(IPlotWorld object) {

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

    public static void registerPlaceholders(ILangModule mod) {

        mod.registerInlinePlaceholderSupplier("creativeplots_plotworld_id",   PlaceholderSupplier.create(IPlotWorld.class, pw -> CreativePlotsAPI.getInstance().getPlotWorldId(pw).toString()));
        mod.registerInlinePlaceholderSupplier("creativeplots_plotworld_name", PlaceholderSupplier.create(IPlotWorld.class, pw -> CreativePlotsAPI.getInstance().getPlotWorldId(pw).getPath()));

    }

}
