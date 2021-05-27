package me.m1dnightninja.creativeplots.api.plot;

import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.math.Vec3i;
import me.m1dnightninja.midnightcore.api.player.MPlayer;


public interface IPlotWorld {

    /**
     * Gets the size of each plot in the world
     *
     * @return The size of each plot as an it
     */
    int getPlotSize();

    /**
     * Gets the width of the roads in the world
     *
     * @return The width of the roads as an int
     */
    int getRoadSize();

    /**
     * Gets the lowest point players can build or break
     *
     * @return The floor's Y-coordinate as an int
     */
    int getWorldFloor();

    /**
     * Gets the height of the world in which players can build
     *
     * @return The height as an int
     */
    int getWorldHeight();

    /**
     * Gets the height of each plot
     *
     * @return The height of each plot when it was originally generated
     */
    int getGenerationHeight();

    /**
     * Gets the spawn location in the world
     *
     * @return The spawn location as a Vec3d
     */
    Vec3d getSpawnLocation();

    /**
     * Converts a PlotPos to a location in the world
     *
     * @return The Plot location as a Vec3d
     */
    Vec3i toLocation(PlotPos position);

    /**
     *
     * @param pl
     * @return
     */
    boolean canInteract(MPlayer pl, Vec3i block);


    /**
     *
     * @param pl
     * @return
     */
    boolean canModify(MPlayer pl, Vec3i block);


    /**
     * Gets the plot registry associated with this world
     *
     * @return The registry as a PlotRegistry
     */
    IPlotRegistry getPlotRegistry();

}
