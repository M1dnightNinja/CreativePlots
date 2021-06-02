package me.m1dnightninja.creativeplots.fabric;

import me.m1dnightninja.creativeplots.api.CreativePlotsAPI;
import me.m1dnightninja.creativeplots.api.plot.IPlotWorld;
import me.m1dnightninja.creativeplots.fabric.integration.WorldEditIntegration;
import me.m1dnightninja.midnightcore.api.math.Vec3i;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.registry.MIdentifier;
import me.m1dnightninja.midnightcore.fabric.api.event.*;
import me.m1dnightninja.midnightcore.fabric.event.Event;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;
import me.m1dnightninja.midnightcore.fabric.util.ConversionUtil;
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
        Event.register(PlayerTeleportEvent.class, this, this::onTeleport);
        Event.register(PlayerJoinedEvent.class, this, this::onJoin);
        Event.register(PlayerDisconnectEvent.class, this, this::onLeave);

        if(FabricLoader.getInstance().isModLoaded("worldedit")) {

            WorldEditIntegration.registerEvents();
        }

    }

    private void onBreak(BlockBreakEvent event) {

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(event.getPlayer());
        if(!pw.canModify(event.getPlayer(), new Vec3i(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()))) event.setCancelled(true);
    }

    private void onPlace(BlockPlaceEvent event) {

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(event.getPlayer());
        if(!pw.canModify(event.getPlayer(), new Vec3i(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()))) event.setCancelled(true);
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

        FabricPlayer pl = (FabricPlayer) FabricPlayer.wrap(event.getPlayer());
        Vec3i loc;

        if(event.getBlockHit() == null) {
            loc = pl.getLocation().truncate();
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
        if(!pw.canInteract(pl, loc)) {
            event.setCancelled(true);
            return;
        }

        if(event.getItem().isEmpty()) return;

        MIdentifier id = ConversionUtil.fromResourceLocation(Registry.ITEM.getKey(event.getItem().getItem()));
        if(!pw.canModify(pl, loc) && !CreativePlotsAPI.getInstance().isAllowedItem(id)) {
            event.setCancelled(true);
        }

    }

    private void onTick(ServerTickEvent event) {

        for(IPlotWorld world : CreativePlotsAPI.getInstance().getWorlds()) {
            world.onTick();
        }
    }

    private void onJoin(PlayerJoinedEvent event) {
        MPlayer pl = FabricPlayer.wrap(event.getPlayer());
        IPlotWorld world = CreativePlotsAPI.getInstance().getPlotWorld(pl);

        if(world != null) world.onEnteredWorld(pl);
    }

    private void onTeleport(PlayerTeleportEvent event) {

        MPlayer pl = FabricPlayer.wrap(event.getPlayer());
        IPlotWorld oldWorld = CreativePlotsAPI.getInstance().getPlotWorld(ConversionUtil.fromResourceLocation(event.getFrom().getWorldId()));
        IPlotWorld newWorld = CreativePlotsAPI.getInstance().getPlotWorld(ConversionUtil.fromResourceLocation(event.getTo().getWorldId()));

        if(oldWorld == newWorld) return;

        if(oldWorld != null) oldWorld.onLeftWorld(pl);
        if(newWorld != null) newWorld.onEnteredWorld(pl);
    }

    private void onLeave(PlayerDisconnectEvent event) {
        MPlayer pl = FabricPlayer.wrap(event.getPlayer());
        IPlotWorld world = CreativePlotsAPI.getInstance().getPlotWorld(pl);

        if(world != null) world.onLeftWorld(pl);
    }

}
