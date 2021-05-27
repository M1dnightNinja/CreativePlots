package me.m1dnightninja.creativeplots.common;

import me.m1dnightninja.creativeplots.api.plot.IPlot;
import me.m1dnightninja.creativeplots.api.plot.IPlotRegistry;
import me.m1dnightninja.creativeplots.api.plot.PlotPos;

import java.util.*;

public class PlotRegistry implements IPlotRegistry {

    private final HashMap<PlotPos, IPlot> registeredPlots;


    protected PlotRegistry() {

        this.registeredPlots = new HashMap<>();
    }


    @Override
    public IPlot getPlotAt(PlotPos plot) {

        for(Map.Entry<PlotPos, IPlot> ent : registeredPlots.entrySet()) {
            if(ent.getKey().equals(plot)) return ent.getValue();
        }

        return null;
    }

    @Override
    public void registerPlot(IPlot plot, PlotPos pos) {

        registeredPlots.put(pos, plot);
    }

    @Override
    public int getSize() {
        return registeredPlots.size();
    }

    @Override
    public Iterator<IPlot> iterator() {
        return new Iterator<IPlot>() {

            private int index = 0;
            private final List<IPlot> plots = new ArrayList<>(registeredPlots.values());

            @Override
            public boolean hasNext() {
                return index < registeredPlots.size();
            }

            @Override
            public IPlot next() {
                IPlot plot = plots.get(index);
                index++;

                return plot;
            }
        };
    }
}
