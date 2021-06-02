package me.m1dnightninja.creativeplots.api.plot;

public interface IPlotRegistry extends Iterable<IPlot> {

    IPlot getPlotAt(PlotPos plot);

    void registerPlot(IPlot plot, PlotPos pos);

    void unregisterPlot(PlotPos pos);

    void unregisterPlot(IPlot pos);

    IPlot getPlot(String id);

    int getSize();

}
