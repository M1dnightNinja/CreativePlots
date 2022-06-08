package org.wallentines.creativeplots.api.plot;

import org.wallentines.midnightlib.math.Vec3d;
import org.wallentines.midnightlib.math.Vec3i;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightlib.registry.Identifier;


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

    Identifier getBorderBlock(PlotPos position);

    Identifier getUnclaimedBorderBlock();

    Identifier getClaimedBorderBlock();

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
     * Queries whether or not a player can use items or blocks at a particular location
     *
     * @param pl    The player in question
     * @param block The block the player clicked on
     * @return      Whether or not the player can interact with the item/block
     */
    boolean canInteract(MPlayer pl, Vec3i block);


    /**
     * Queries whether or not a player place or break blocks at a particular location
     *
     * @param pl    The player in question
     * @param block The location where the block will be placed or broken
     * @return      Whether or not the player can modify at that location
     */
    boolean canModify(MPlayer pl, Vec3i block);


    /**
     * Gets the plot registry associated with this world
     *
     * @return The registry as a PlotRegistry
     */
    IPlotRegistry getPlotRegistry();

    IPlot getPlot(Vec3i block);


    void onEnteredWorld(MPlayer player);
    void onLeftWorld(MPlayer player);
    void onTick();

}
