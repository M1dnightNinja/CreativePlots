package org.wallentines.creativeplots.fabric;

import org.wallentines.creativeplots.api.CreativePlotsAPI;
import org.wallentines.creativeplots.api.plot.IPlotWorld;
import org.wallentines.creativeplots.fabric.integration.WorldEditIntegration;
import org.wallentines.midnightcore.fabric.event.player.*;
import org.wallentines.midnightcore.fabric.event.server.ServerTickEvent;
import org.wallentines.midnightcore.fabric.event.world.BlockBreakEvent;
import org.wallentines.midnightcore.fabric.event.world.BlockPlaceEvent;
import org.wallentines.midnightcore.fabric.event.world.ExplosionEvent;
import org.wallentines.midnightcore.fabric.event.world.PortalCreateEvent;
import org.wallentines.midnightlib.event.Event;
import org.wallentines.midnightlib.math.Vec3i;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightcore.fabric.player.FabricPlayer;
import org.wallentines.midnightcore.fabric.util.ConversionUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.BucketItem;

public class PlotListener {

    public void register() {

        Event.register(BlockBreakEvent.class, this, this::onBreak);
        Event.register(BlockPlaceEvent.class, this, this::onPlace);
        Event.register(ExplosionEvent.class, this, this::onExplode);
        Event.register(PlayerInteractEvent.class, this, this::onUse);
        Event.register(ServerTickEvent.class, this, this::onTick);
        Event.register(PlayerChangeDimensionEvent.class, this, this::onDimension);
        Event.register(PlayerJoinEvent.class, this, this::onJoin);
        Event.register(PlayerLeaveEvent.class, this, this::onLeave);
        Event.register(PortalCreateEvent.class, this, this::onPortal);

        if(FabricLoader.getInstance().isModLoaded("worldedit")) {

            WorldEditIntegration.registerEvents();
        }

    }

    private void onBreak(BlockBreakEvent event) {

        FabricPlayer fp = FabricPlayer.wrap(event.getPlayer());
        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(fp);
        if(pw != null && !pw.canModify(fp, new Vec3i(event.getPosition().getX(), event.getPosition().getY(), event.getPosition().getZ()))) event.setCancelled(true);
    }

    private void onPlace(BlockPlaceEvent event) {

        FabricPlayer fp = FabricPlayer.wrap(event.getPlayer());
        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(fp);
        if(pw != null && !pw.canModify(fp, new Vec3i(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()))) event.setCancelled(true);
    }

    private void onExplode(ExplosionEvent event) {

        MPlayer source = null;
        if(event.getSource() != null && event.getSource() instanceof PrimedTnt) {

            Entity owner = ((PrimedTnt) event.getSource()).getOwner();
            if(owner instanceof ServerPlayer) {

                source = FabricPlayer.wrap((ServerPlayer) owner);
            }
        }

        if(source == null) {
            event.setCancelled(true);

        } else {

            IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(source);
            if(pw == null) return;

            for(int i = 0 ; i < event.getAffectedBlocks().size() ; i++) {
                BlockPos pos = event.getAffectedBlocks().get(i);
                if(!pw.canModify(source, new Vec3i(pos.getX(), pos.getY(), pos.getZ()))) {
                    event.getAffectedBlocks().remove(i);
                    i--;
                }
            }
        }
    }

    private void onUse(PlayerInteractEvent event) {

        FabricPlayer pl = FabricPlayer.wrap(event.getPlayer());
        Vec3i loc;

        if(event.getBlockHit() == null) {
            loc = pl.getLocation().getCoordinates().truncate();
        } else {
            BlockPos b;
            if(!event.getItem().isEmpty() && event.getItem().getItem() instanceof BucketItem) {
                b = event.getBlockHit().getBlockPos();
            } else {
                b = event.getBlockHit().getBlockPos().relative(event.getBlockHit().getDirection());
            }
            loc = new Vec3i(b.getX(), b.getY(), b.getZ());
        }

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) return;

        if(!pw.canInteract(pl, loc)) {
            event.setCancelled(true);
            return;
        }

        if(event.getItem().isEmpty()) return;

        Identifier id = ConversionUtil.toIdentifier(Registry.ITEM.getKey(event.getItem().getItem()));
        if(!pw.canModify(pl, loc) && !CreativePlotsAPI.getInstance().isAllowedItem(id)) {
            event.setCancelled(true);
        }

    }

    private void onTick(ServerTickEvent event) {

        for(IPlotWorld world : CreativePlotsAPI.getInstance().getWorlds()) {
            world.onTick();
        }
    }

    private void onJoin(PlayerJoinEvent event) {
        MPlayer pl = FabricPlayer.wrap(event.getPlayer());
        IPlotWorld world = CreativePlotsAPI.getInstance().getPlotWorld(pl);

        if(world != null) world.onEnteredWorld(pl);
    }

    private void onLeave(PlayerLeaveEvent event) {
        MPlayer pl = FabricPlayer.wrap(event.getPlayer());
        IPlotWorld world = CreativePlotsAPI.getInstance().getPlotWorld(pl);

        if(world != null) world.onLeftWorld(pl);
    }

    private void onPortal(PortalCreateEvent event) {

        IPlotWorld world = CreativePlotsAPI.getInstance().getPlotWorld(ConversionUtil.toIdentifier(event.getSourceDimension().dimension().location()));
        if(world != null) event.setCancelled(true);
    }

    private void onDimension(PlayerChangeDimensionEvent event) {

        IPlotWorld world = CreativePlotsAPI.getInstance().getPlotWorld(ConversionUtil.toIdentifier(event.getOldLevel().dimension().location()));
        IPlotWorld newWorld = CreativePlotsAPI.getInstance().getPlotWorld(ConversionUtil.toIdentifier(event.getNewLevel().dimension().location()));
        if(world == newWorld) return;

        FabricPlayer fp = FabricPlayer.wrap(event.getPlayer());

        if(world != null) world.onLeftWorld(fp);
        if(newWorld != null) newWorld.onEnteredWorld(fp);

    }

}
