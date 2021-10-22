package me.m1dnightninja.creativeplots.api.plot;

import java.util.List;

public interface IPlotRegistry extends Iterable<IPlot> {

    IPlot getPlotAt(PlotPos plot);

    void registerPlot(IPlot plot, PlotPos pos);

    void unregisterPlot(PlotPos pos);

    void unregisterPlot(IPlot pos);

    IPlot getPlot(String id);

    List<IPlot> getUniquePlots();

    int getSize();

}
