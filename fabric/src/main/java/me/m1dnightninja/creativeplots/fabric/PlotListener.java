package me.m1dnightninja.creativeplots.fabric;

import me.m1dnightninja.creativeplots.api.CreativePlotsAPI;
import me.m1dnightninja.creativeplots.api.plot.IPlotWorld;
import me.m1dnightninja.midnightcore.api.math.Vec3i;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.fabric.api.event.BlockBreakEvent;
import me.m1dnightninja.midnightcore.fabric.api.event.BlockPlaceEvent;
import me.m1dnightninja.midnightcore.fabric.api.event.ExplosionEvent;
import me.m1dnightninja.midnightcore.fabric.event.Event;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;

public class PlotListener {

    public void register() {

        Event.register(BlockBreakEvent.class, this, this::onBreak);
        Event.register(BlockPlaceEvent.class, this, this::onPlace);
        Event.register(ExplosionEvent.class, this, this::onExplode);
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

        if(source == null) event.setCancelled(true);

    }

}
