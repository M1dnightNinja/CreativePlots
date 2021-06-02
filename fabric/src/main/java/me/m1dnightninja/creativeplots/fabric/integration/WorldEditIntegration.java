package me.m1dnightninja.creativeplots.fabric.integration;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.event.Event;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import me.m1dnightninja.creativeplots.api.CreativePlotsAPI;
import me.m1dnightninja.creativeplots.api.plot.IPlot;
import me.m1dnightninja.creativeplots.api.plot.IPlotWorld;
import me.m1dnightninja.creativeplots.api.plot.PlotPos;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.math.Vec3i;
import me.m1dnightninja.midnightcore.api.player.MPlayer;

import java.util.UUID;

public class WorldEditIntegration {

    private static class PlotExtent extends AbstractDelegateExtent {

        private final Iterable<me.m1dnightninja.creativeplots.api.math.Region> regions;

        public static final BaseBlock AIR = BlockTypes.AIR.getDefaultState().toBaseBlock();
        public static final BlockState AIR_STATE = BlockTypes.AIR.getDefaultState();

        protected PlotExtent(Extent extent, Iterable<me.m1dnightninja.creativeplots.api.math.Region> regions) {
            super(extent);
            this.regions = regions;
        }

        private boolean blockWithin(int x, int y, int z) {
            Vec3i vec3i = new Vec3i(x,y,z);
            for(me.m1dnightninja.creativeplots.api.math.Region reg : regions) {
                if(reg.contains(vec3i)) return true;
            }
            return false;
        }

        @Override
        public boolean setBlock(BlockVector3 location, BlockStateHolder block) throws WorldEditException {
            return blockWithin(location.getBlockX(), location.getBlockY(), location.getBlockZ()) && super.setBlock(location, block);
        }

        @Override
        public Entity createEntity(Location location, BaseEntity entity) {
            if (blockWithin(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
                return super.createEntity(location, entity);
            }
            return null;
        }

        @Override
        public boolean setBiome(BlockVector2 position, BiomeType biome) {
            return blockWithin(position.getX(), 64, position.getZ()) && super.setBiome(position, biome);
        }

        @Override
        public BlockState getBlock(BlockVector3 location) {
            if (blockWithin(location.getX(), location.getY(), location.getZ())) {
                return super.getBlock(location);
            }
            return AIR_STATE;
        }

        @Override
        public BaseBlock getFullBlock(BlockVector3 location) {
            if (blockWithin(location.getX(), location.getY(), location.getZ())) {
                return super.getFullBlock(location);
            }
            return AIR;
        }
    }

    public static void registerEvents() {

        WorldEdit.getInstance().getEventBus().register(new Object() {
            @Subscribe
            public void sessionCallback(EditSessionEvent event) {

                if(event.getActor() == null || !event.getActor().isPlayer()) return;
                AbstractPlayerActor act = (AbstractPlayerActor) event.getActor();
                UUID u = act.getUniqueId();

                MPlayer player = MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(u);

                if(player.hasPermission("creativeplots.editanywhere")) return;

                IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(player);
                Vec3i loc = player.getLocation().truncate();

                if(pw == null) return;
                IPlot plot = pw.getPlot(loc);

                if(plot == null || !plot.canEdit(player)) {
                    event.setExtent(new NullExtent());
                    return;
                }
                event.setExtent(new PlotExtent(event.getExtent(), plot.getArea()));
            }
        });

    }

}
