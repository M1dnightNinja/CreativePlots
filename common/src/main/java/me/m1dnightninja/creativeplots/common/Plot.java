package me.m1dnightninja.creativeplots.common;

import me.m1dnightninja.creativeplots.api.plot.IPlot;
import me.m1dnightninja.creativeplots.api.plot.IPlotRegistry;
import me.m1dnightninja.creativeplots.api.plot.IPlotWorld;
import me.m1dnightninja.creativeplots.api.plot.PlotPos;
import me.m1dnightninja.creativeplots.api.math.Region;
import me.m1dnightninja.midnightcore.api.config.ConfigSection;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.math.Vec3i;
import me.m1dnightninja.midnightcore.api.player.MPlayer;

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
    private String friendlyName;

    private final List<UUID> trusted;
    private final List<Region> area;

    // Create a default plot at a given position
    protected Plot(IPlotWorld map, String id, PlotPos... pos) {

        this.map = map;

        if(pos.length == 0) {
            throw new IllegalStateException("Unable to create plot! No valid positions found!");
        }

        this.id = id;
        this.positions = Arrays.asList(pos);

        this.trusted = new ArrayList<>();
        this.area = new ArrayList<>();

        if(pos.length > 1) {
            calculateArea();
        } else {
            area.add(positions.get(0).toRegion(map));
        }
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
    public String getName() {
        return friendlyName;
    }

    @Override
    public boolean canEdit(MPlayer user) {
        UUID u = user.getUUID();
        return u.equals(owner) || trusted.contains(u) || user.hasPermission("creativeplots.editanywhere");
    }

    @Override
    public Vec3d getTeleportLocation() {
        return new Vec3d(0,0,0);
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
    public void setName(String name) {
        this.friendlyName = name;
    }


    private void calculateArea() {

        int minX = positions.get(0).getX();
        int minZ = positions.get(0).getZ();
        int maxX = minX;
        int maxZ = minZ;

        for(PlotPos pos1 : positions) {

            if(pos1.getX() < minX) {
                minX = pos1.getX();
            } else if(pos1.getX() > maxX) {
                maxX = pos1.getX();
            }

            if(pos1.getZ() < minZ) {
                minZ = pos1.getZ();
            } else if(pos1.getZ() > maxZ) {
                maxX = pos1.getZ();
            }
        }

        int[][] map = new int[maxX-minX][maxZ-minZ];

        for(int z = 0 ; z < map.length ; z++) {
            for(int x = 0 ; x < map[0].length ; x++) {
                if(positions.contains(new PlotPos(x,z))) {
                    map[x][z] = 1;
                } else {
                    map[x][z] = 0;
                }
            }
        }


        for(int iz = minX ; iz < maxX ; iz++) {
            for(int ix = minZ ; ix < maxZ ; ix++) {

                int x = ix;
                int z = iz;

                Vec3i c1 = null;
                Vec3i c2 = null;

                boolean[] canExpand = { true, true, true, true };

                if(map[x][z] == 1) {
                    c1 = new Vec3i(x, this.map.getWorldFloor(),z);
                    c2 = new Vec3i(x, this.map.getWorldHeight(), z);
                }

                while(true) {

                    if(x == minX || map[x-1][z] == 0) {
                        canExpand[0] = false;
                    }
                    if(z == minZ || map[x][z-1] == 0) {
                        canExpand[1] = false;
                    }
                    if(x == maxX || map[x+1][z] == 0) {
                        canExpand[2] = false;
                    }
                    if(z == maxZ || map[x][z+1] == 0) {
                        canExpand[3] = false;
                    }

                    if(canExpand[0]) {
                        x -= 1;
                        c1.add(new Vec3i(-1,0,0));
                    }
                    if(canExpand[1]) {
                        z -= 1;
                        c1.add(new Vec3i(0,0,-1));
                    }
                    if(canExpand[2]) {
                        x += 1;
                        c2.add(new Vec3i(1,0,0));
                    }
                    if(canExpand[3]) {
                        z += 1;
                        c2.add(new Vec3i(0,0,1));
                    }

                    for(int dx = c1.getX() ; dx < c2.getX() ; dx++) {
                        for(int dz = c2.getZ() ; dz < c2.getZ() ; dz++) {
                            map[dx][dz] = 2;
                        }
                    }

                    if(!(canExpand[0] || canExpand[1] || canExpand[2] || canExpand[3])) {

                        Region r1 = new PlotPos(c1.getX(), c1.getZ()).toRegion(this.map);
                        Region r2 = new PlotPos(c2.getX(), c2.getZ()).toRegion(this.map);

                        Region out = new Region(r1.getLowerBound(), r2.getUpperBound().subtract(r1.getLowerBound()));
                        area.add(out);

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
            out.friendlyName = section.getString("name");
        }

        if(section.has("trusted", List.class)) {
            List<String> list = section.getStringList("trusted");
            for(String s : list) {
                out.trusted.add(UUID.fromString(s));
            }
        }

        return out;
    }

    public ConfigSection serialize() {

        ConfigSection out = new ConfigSection();

        out.set("id", id);
        out.set("owner", owner.toString());
        out.set("name", friendlyName);

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

        return out;

    }

}
