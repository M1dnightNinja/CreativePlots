package org.wallentines.creativeplots.common;

import org.wallentines.creativeplots.api.CreativePlotsAPI;
import org.wallentines.creativeplots.api.event.PlotEnterEvent;
import org.wallentines.creativeplots.api.event.PlotLeaveEvent;
import org.wallentines.creativeplots.api.math.Region;
import org.wallentines.midnightcore.api.MidnightCoreAPI;
import org.wallentines.midnightcore.api.player.Location;
import org.wallentines.midnightcore.api.text.MTextComponent;
import org.wallentines.midnightlib.config.ConfigSection;
import org.wallentines.midnightlib.event.Event;
import org.wallentines.midnightlib.math.Color;
import org.wallentines.midnightlib.math.Vec3d;
import org.wallentines.midnightlib.math.Vec3i;
import org.wallentines.midnightcore.api.module.lang.LangModule;
import org.wallentines.midnightcore.api.module.lang.PlaceholderSupplier;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightcore.api.text.MComponent;
import org.wallentines.midnightcore.api.text.MStyle;
import org.wallentines.creativeplots.api.plot.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class Plot implements IPlot {

    private static final Pattern lint = Pattern.compile("[a-z0-9_.\\-:;,]+");

    private final List<PlotPos> positions;
    private final IPlotWorld map;

    private final String id;

    private UUID owner;
    private MComponent friendlyName;

    private final List<UUID> trusted;
    private final List<UUID> denied;
    private final List<Region> area;

    // Create a default plot at a given position
    public Plot(IPlotWorld map, String id, PlotPos... pos) {

        this.map = map;

        if(pos.length == 0) {
            throw new IllegalStateException("Unable to create plot! No valid positions found!");
        }

        this.id = id;
        this.friendlyName = new MTextComponent(id).withStyle(new MStyle().withColor(Color.fromRGBI(6)));
        this.positions = new ArrayList<>();

        positions.addAll(Arrays.asList(pos));

        this.trusted = new ArrayList<>();
        this.denied = new ArrayList<>();
        this.area = new ArrayList<>();

        calculateArea();
    }

    public void register(IPlotRegistry reg) {
        for(PlotPos pos : positions) {
            reg.registerPlot(this, pos);
        }
    }


    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public MComponent getName() {
        return friendlyName;
    }

    @Override
    public boolean canEdit(MPlayer user) {
        UUID u = user.getUUID();
        return u.equals(owner) || trusted.contains(u) || user.hasPermission("creativeplots.editanywhere", 4);
    }

    @Override
    public Vec3d getTeleportLocation() {
        Vec3i blockPos = map.toLocation(positions.get(0));
        return new Vec3d(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
    }

    @Override
    public boolean contains(Vec3i location) {

        for(Region reg : area) {
            if(reg.contains(location)) return true;
        }

        return false;
    }

    @Override
    public void setOwner(UUID user) {
        this.owner = user;
    }

    @Override
    public void setName(MComponent name) {
        this.friendlyName = name;
    }

    @Override
    public void merge(IPlot other) {

        for(PlotPos pos : other.getPositions()) {
            if(!positions.contains(pos)) {
                positions.add(pos);
            }
        }

        calculateArea();
    }

    @Override
    public List<Region> getArea() {
        return area;
    }

    @Override
    public List<PlotPos> getPositions() {
        return positions;
    }

    @Override
    public boolean isDenied(UUID u) {
        return denied.contains(u);
    }

    @Override
    public void trustPlayer(UUID u) {
        if(u.equals(owner) || trusted.contains(u)) return;

        trusted.add(u);
    }

    @Override
    public void untrustPlayer(UUID u) {
        trusted.remove(u);
    }

    @Override
    public void denyPlayer(UUID u) {
        if(u.equals(owner) || denied.contains(u)) return;

        denied.add(u);
        MPlayer pl = MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(u);


        int offset = map.getRoadSize() / 2;
        if(map.getRoadSize() % 2 == 1) offset += 1;

        if(contains(pl.getLocation().getCoordinates().truncate())) {
            pl.teleport(new Location(pl.getLocation().getWorldId(), getTeleportLocation().add(new Vec3d(-1 * offset, 1, -1 * offset)), 0, 0));
        }
    }

    @Override
    public void undenyPlayer(UUID u) {
        denied.remove(u);
    }

    @Override
    public boolean hasOwnerPermissions(MPlayer player) {
        return player.getUUID().equals(owner) || player.hasPermission("creativeplots.ownereverywhere", 4);
    }

    @Override
    public MComponent getOwnerName() {

        return getOwnerName(null);
    }

    @Override
    public void onEnter(MPlayer player) {
        MComponent title = CreativePlotsAPI.getInstance().getLangProvider().getMessage("plot.title", player, player, this, map);
        MComponent subtitle = CreativePlotsAPI.getInstance().getLangProvider().getMessage("plot.subtitle", player, player, this, map);

        player.sendTitle(title, 20, 80, 20);
        player.sendSubtitle(subtitle, 20, 80, 20);

        Event.invoke(new PlotEnterEvent(player, this));
    }

    @Override
    public void onLeave(MPlayer player) {

        Event.invoke(new PlotLeaveEvent(player, this));
    }

    private MComponent getOwnerName(MPlayer pl) {

        if (owner == null) return CreativePlotsAPI.getInstance().getLangProvider().getMessage("plot.null_owner", pl);

        MPlayer player = MidnightCoreAPI.getInstance().getPlayerManager().getPlayer(owner);
        return player.getName();
    }

    private void calculateArea() {

        area.clear();

        if(positions.size() == 1) {
            area.add(positions.get(0).toRegion(map));
            return;
        }

        int minX = positions.get(0).getX();
        int minZ = positions.get(0).getZ();
        int maxX = minX;
        int maxZ = minZ;

        for(PlotPos pos : positions) {
            if(pos.getX() < minX) minX = pos.getX();
            if(pos.getX() > maxX) maxX = pos.getX();
            if(pos.getZ() < minZ) minZ = pos.getZ();
            if(pos.getZ() > maxZ) maxZ = pos.getZ();
        }


        int xCount = maxX - minX + 1;
        int zCount = maxZ - minZ + 1;

        int[][] map = new int[xCount][zCount];

        for(int x = 0 ; x < xCount ; x++) {
            for(int z = 0 ; z < zCount ; z++) {
                PlotPos pos = new PlotPos(x + minX, z + minZ);
                map[x][z] = positions.contains(pos) ? 1 : 0;
            }
        }

        for(int x = 0 ; x < xCount ; x++) {
            for(int z = 0 ; z < zCount ; z++) {

                int state = map[x][z];

                if(state == 1) {

                    int ix = x;
                    int iz = z;

                    boolean[] canExpand = new boolean[] { true, true, true, true };
                    PlotPos lower = new PlotPos(x, z);
                    PlotPos higher = new PlotPos(x, z);

                    while(true) {

                        if(ix == 0 || map[ix - 1][higher.getZ()] == 0) canExpand[0] = false;
                        if(iz == 0 || map[higher.getX()][iz - 1] == 0) canExpand[1] = false;
                        if(ix + 1 == xCount || map[ix + 1][higher.getZ()] == 0) canExpand[2] = false;
                        if(iz + 1 == zCount || map[higher.getX()][iz + 1] == 0) canExpand[3] = false;

                        if(canExpand[0]) {
                            ix -= 1;
                            lower = lower.getAdjacent(PlotDirection.WEST);
                            continue;
                        }
                        if(canExpand[1]) {
                            iz -= 1;
                            lower = lower.getAdjacent(PlotDirection.NORTH);
                            continue;
                        }
                        if(canExpand[2]) {
                            ix += 1;
                            higher = higher.getAdjacent(PlotDirection.EAST);
                            continue;
                        }
                        if(canExpand[3]) {
                            iz += 1;
                            higher = higher.getAdjacent(PlotDirection.SOUTH);
                            continue;
                        }

                        for(int rx = lower.getX() ; rx < higher.getX() + 1; rx++) {
                            for(int rz = lower.getZ() ; rz < higher.getZ() + 1 ; rz++) {
                                map[rx][rz] = 2;
                            }
                        }

                        Region l = new PlotPos(lower.getX() + minX, lower.getZ() + minZ).toRegion(this.map);
                        Region h = new PlotPos(higher.getX() + minX, higher.getZ() + minZ).toRegion(this.map);
                        area.add(new Region(l.getLowerBound(), h.getUpperBound().subtract(l.getLowerBound())));

                        break;
                    }
                }
            }
        }
    }


    /**
     * Parses a config section to create a Plot Object
     *
     * @param section A configuration section to parse
     * @return        A new plot object
     * @throws IllegalStateException if the config section does not contain enough information
     */
    public static Plot fromConfig(IPlotWorld map, ConfigSection section) throws IllegalStateException {

        List<ConfigSection> poss = section.getList("positions", ConfigSection.class);

        PlotPos[] positions = new PlotPos[poss.size()];

        for(int i = 0 ; i < poss.size() ; i++) {
            ConfigSection sec = poss.get(i);

            final int x = sec.getInt("x");
            final int z = sec.getInt("z");

            positions[i] = new PlotPos(x,z);
        }

        String id = section.getString("id");

        if(!lint.matcher(id).matches()) {
            throw new IllegalStateException("Unable to parse plot! Invalid ID!");
        }

        Plot out = new Plot(map, id, positions);

        if(section.has("owner")) {
            out.owner = UUID.fromString(section.getString("owner"));
        }

        if(section.has("name")) {
            out.friendlyName = MComponent.parse(section.getString("name"));
        }

        if(section.has("trusted", List.class)) {
            List<String> list = section.getListFiltered("trusted", String.class);
            for(String s : list) {
                out.trusted.add(UUID.fromString(s));
            }
        }

        if(section.has("denied", List.class)) {
            List<String> list = section.getListFiltered("denied", String.class);
            for(String s : list) {
                out.denied.add(UUID.fromString(s));
            }
        }

        return out;
    }

    public ConfigSection serialize() {

        ConfigSection out = new ConfigSection();

        out.set("id", id);
        out.set("name", friendlyName);

        if(owner != null) {
            out.set("owner", owner.toString());
        }

        List<ConfigSection> poss = new ArrayList<>();
        for(PlotPos pos : positions) {
            ConfigSection sec = new ConfigSection();
            sec.set("x", pos.getX());
            sec.set("z", pos.getZ());
            poss.add(sec);
        }
        out.set("positions", poss);

        if(trusted.size() > 0) {
            List<String> trust = new ArrayList<>();
            for (UUID u : trusted) {
                trust.add(u.toString());
            }
            out.set("trusted", trust);
        }

        if(denied.size() > 0) {
            List<String> deny = new ArrayList<>();
            for (UUID u : denied) {
                deny.add(u.toString());
            }
            out.set("denied", deny);
        }

        return out;

    }

    @Override
    public Integer getTimeOfDay() {
        return 6000;
    }

    @Override
    public boolean isRaining() {
        return false;
    }


    public static void registerPlaceholders(LangModule mod) {

        mod.registerInlinePlaceholder("creativeplots_plot_id", PlaceholderSupplier.create(IPlot.class, IPlot::getId));
        mod.registerPlaceholder("creativeplots_plot_name", PlaceholderSupplier.create(IPlot.class, IPlot::getName));

        mod.registerPlaceholder("creativeplots_plot_owner", args -> {
            MPlayer pl = null;
            Plot plot = null;
            for(Object o : args) {
                if(o instanceof MPlayer) {
                    pl = (MPlayer) o;
                    continue;
                }
                if(o instanceof Plot) {
                    plot = (Plot) o;
                }
            }
            if(plot == null) return null;

            return plot.getOwnerName(pl);
        });
    }

}
