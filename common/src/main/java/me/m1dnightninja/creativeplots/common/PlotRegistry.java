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
            if(ent.getKey().equals(plot)) {
                return ent.getValue();
            }
        }

        return null;
    }

    @Override
    public void registerPlot(IPlot plot, PlotPos pos) {

        registeredPlots.put(pos, plot);
    }

    @Override
    public void unregisterPlot(PlotPos pos) {
        IPlot plot = getPlotAt(pos);
        if(plot == null) return;

        unregisterPlot(plot);
    }

    @Override
    public void unregisterPlot(IPlot plot) {

        for(PlotPos p : plot.getPositions()) {
            registeredPlots.remove(p);
        }
    }

    @Override
    public IPlot getPlot(String id) {
        for(IPlot plot : registeredPlots.values()) {
            if(plot.getId().equals(id)) return plot;
        }
        return null;
    }

    @Override
    public int getSize() {
        return registeredPlots.size();
    }

    @Override
    public List<IPlot> getUniquePlots() {
        List<IPlot> out = new ArrayList<>();
        for(IPlot p : registeredPlots.values()) {
            if(!out.contains(p)) out.add(p);
        }
        return out;
    }

    @Override
    public Iterator<IPlot> iterator() {
        return getUniquePlots().iterator();
    }
}
