package org.wallentines.creativeplots.api.event;

import org.wallentines.creativeplots.api.plot.IPlot;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightlib.event.Event;

public class PlotEnterEvent extends Event {

    private final MPlayer player;
    private final IPlot plot;

    public PlotEnterEvent(MPlayer player, IPlot plot) {
        this.player = player;
        this.plot = plot;
    }

    public MPlayer getPlayer() {
        return player;
    }

    public IPlot getPlot() {
        return plot;
    }
}
