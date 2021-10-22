package me.m1dnightninja.creativeplots.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.m1dnightninja.creativeplots.api.CreativePlotsAPI;
import me.m1dnightninja.creativeplots.api.math.Region;
import me.m1dnightninja.creativeplots.api.plot.IPlot;
import me.m1dnightninja.creativeplots.api.plot.IPlotWorld;
import me.m1dnightninja.creativeplots.api.plot.PlotDirection;
import me.m1dnightninja.creativeplots.api.plot.PlotPos;
import me.m1dnightninja.creativeplots.common.Plot;
import me.m1dnightninja.creativeplots.fabric.generator.PlotworldGenerator;
import me.m1dnightninja.midnightcore.api.MidnightCoreAPI;
import me.m1dnightninja.midnightcore.api.inventory.MInventoryGUI;
import me.m1dnightninja.midnightcore.api.inventory.MItemStack;
import me.m1dnightninja.midnightcore.api.math.Color;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.math.Vec3i;
import me.m1dnightninja.midnightcore.api.module.lang.CustomPlaceholderInline;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.text.MComponent;
import me.m1dnightninja.midnightcore.api.text.MStyle;
import me.m1dnightninja.midnightcore.fabric.MidnightCore;
import me.m1dnightninja.midnightcore.fabric.module.lang.LangModule;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;
import me.m1dnightninja.midnightcore.fabric.util.ConversionUtil;
import me.m1dnightninja.midnightcore.fabric.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.ArrayList;
import java.util.List;

public class MainCommand {

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
            Commands.literal("plot")
                .requires(context -> PermissionUtil.checkOrOp(context, "creativeplots.command", 2))
                .then(Commands.literal("claim")
                    .requires(context -> PermissionUtil.checkOrOp(context, "creativeplots.command.claim", 2))
                    .executes(context -> executeClaim(context, context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeClaim(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                    )
                )
                .then(Commands.literal("delete")
                    .requires(context -> PermissionUtil.checkOrOp(context, "creativeplots.command.delete", 2))
                    .executes(this::executeDelete)
                )
                .then(Commands.literal("tp")
                    .requires(context -> PermissionUtil.checkOrOp(context, "creativeplots.command.tp", 2))
                    .then(Commands.argument("plot", StringArgumentType.greedyString())
                        .suggests((context, builder) -> {
                            List<String> out = new ArrayList<>();
                            MPlayer pl = FabricPlayer.wrap(context.getSource().getPlayerOrException());
                            IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);

                            for(IPlot p : pw.getPlotRegistry()) {
                                if(p.getOwner().equals(pl.getUUID())) {
                                    out.add(p.getId());
                                }
                            }

                            return SharedSuggestionProvider.suggest(out, builder);
                        })
                        .executes(context -> executeTp(context, context.getArgument("plot", String.class)))
                    )
                )
                .then(Commands.literal("rename")
                    .requires(context -> PermissionUtil.checkOrOp(context, "creativeplots.command.rename", 2))
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> executeRename(context, context.getArgument("name", String.class)))
                    )
                )
                .then(Commands.literal("trust")
                    .requires(context -> PermissionUtil.checkOrOp(context, "creativeplots.command.trust", 2))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeTrust(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                    )
                )
                .then(Commands.literal("remove")
                    .requires(context -> PermissionUtil.checkOrOp(context, "creativeplots.command.remove", 2))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeRemove(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                    )
                )
                .then(Commands.literal("deny")
                    .requires(context -> PermissionUtil.checkOrOp(context, "creativeplots.command.deny", 2))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeDeny(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                    )
                )
                .then(Commands.literal("allow")
                    .requires(context -> PermissionUtil.checkOrOp(context, "creativeplots.command.allow", 2))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeAllow(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                    )
                )
                .then(Commands.literal("merge")
                    .requires(context -> PermissionUtil.checkOrOp(context, "creativeplots.command.merge", 2))
                    .executes(this::executeMerge)
                )
        );

    }

    private int executeClaim(CommandContext<CommandSourceStack> context, ServerPlayer player) {

        MPlayer pl = FabricPlayer.wrap(player);

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = new Vec3d(player.getX(), player.getY(), player.getZ()).truncate();
        PlotPos pos = PlotPos.fromCoords(location.getX(), location.getZ(), pw.getPlotSize(), pw.getRoadSize());

        if(pos == null) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_location");
            return 0;
        }

        IPlot plot = pw.getPlotRegistry().getPlotAt(pos);
        if(plot != null) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.claimed");
            return 0;
        }

        plot = new Plot(pw, pos.getX() + "," + pos.getZ(), pos);
        plot.setOwner(pl.getUUID());

        ((Plot) plot).register(pw.getPlotRegistry());

        LangModule.sendCommandSuccess(context, CreativePlotsAPI.getInstance().getLangProvider(), false, "command.claim.success");
        plot.sendEnterTitle(pl);

        Region reg = pos.toRegion(pw).outset(1);
        BlockPos.MutableBlockPos bpos = new BlockPos.MutableBlockPos();

        ServerLevel l = context.getSource().getLevel();
        BlockState state = Registry.BLOCK.get(ConversionUtil.toResourceLocation(pw.getClaimedBorderBlock())).defaultBlockState();
        for (int x = reg.getLowerBound().getX(); x < reg.getUpperBound().getX() + 1; x++) {
            for (int z = reg.getLowerBound().getZ(); z < reg.getUpperBound().getZ() + 1; z++) {

                if (!PlotPos.isPlotBorder(x, z, pw.getPlotSize(), pw.getRoadSize())) continue;

                bpos.set(x, pw.getGenerationHeight() + 1, z);
                l.setBlock(bpos, state, 2);
                l.blockUpdated(bpos, state.getBlock());
            }
        }

        return 1;
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(context.getSource().getPlayerOrException());

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = pl.getLocation().truncate();
        IPlot plot = pw.getPlot(location);

        if(plot == null || !plot.hasOwnerPermissions(pl)) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        MInventoryGUI confirm = MidnightCoreAPI.getInstance().createInventoryGUI(CreativePlotsAPI.getInstance().getLangProvider().getMessage("command.delete.gui.title"));
        confirm.setItem(MItemStack.Builder.woolWithColor(Color.fromRGBI(10)).withName(CreativePlotsAPI.getInstance().getLangProvider().getMessage("command.delete.gui.yes")).build(), 3, (type, user) -> {
            doDelete(pw, plot, context.getSource().getLevel());
            LangModule.sendCommandSuccess(context, CreativePlotsAPI.getInstance().getLangProvider(), false, "command.delete.success");
            MInventoryGUI.closeMenu(user);
        });
        confirm.setItem(MItemStack.Builder.woolWithColor(Color.fromRGBI(12)).withName(CreativePlotsAPI.getInstance().getLangProvider().getMessage("command.delete.gui.no")).build(), 5, (type, user) -> {
            MInventoryGUI.closeMenu(user);
        });
        confirm.open(pl, 0);

        return 1;
    }

    private void doDelete(IPlotWorld world, IPlot plot, ServerLevel level) {

        ChunkGenerator gen = level.getChunkSource().getGenerator();
        if(!(gen instanceof PlotworldGenerator)) return;

        for(Region r : plot.getArea()) {
            Region newReg = r.outset(1);
            ((PlotworldGenerator) gen).regenerateRegion(newReg, level);
        }

        world.getPlotRegistry().unregisterPlot(plot);
    }

    private int executeTp(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(context.getSource().getPlayerOrException());

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        IPlot plot = pw.getPlotRegistry().getPlot(name);
        if(plot == null || !plot.hasOwnerPermissions(pl)) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        pl.teleport(plot.getTeleportLocation(), 0, 0);
        return 1;
    }

    private int executeRename(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(context.getSource().getPlayerOrException());

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = pl.getLocation().truncate();
        IPlot plot = pw.getPlot(location);

        if(plot == null || !plot.hasOwnerPermissions(pl)) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        plot.setName(MComponent.createTextComponent(name).withStyle(new MStyle().withColor(Color.fromRGBI(6))));
        LangModule.sendCommandSuccess(context, CreativePlotsAPI.getInstance().getLangProvider(), false, "command.rename.success", new CustomPlaceholderInline("name", name));

        return 1;

    }

    private int executeTrust(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(player);

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = pl.getLocation().truncate();
        IPlot plot = pw.getPlot(location);

        if(plot == null || !plot.hasOwnerPermissions(pl)) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        if(player.equals(context.getSource().getPlayerOrException())) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_self_target");
            return 0;
        }

        if(plot.canEdit(pl)) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_trust_target");
            return 0;
        }

        plot.trustPlayer(player.getUUID());
        LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.trust.success");

        return 1;
    }

    private int executeRemove(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(player);

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = pl.getLocation().truncate();
        IPlot plot = pw.getPlot(location);
        if(plot == null || !plot.hasOwnerPermissions(pl)) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        if(player.equals(context.getSource().getPlayerOrException())) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_self_target");
            return 0;
        }

        if(!plot.canEdit(pl)) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_remove_target");
            return 0;
        }

        plot.untrustPlayer(player.getUUID());
        LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.trust.success");

        return 1;
    }

    private int executeDeny(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(player);

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }
        Vec3i location = pl.getLocation().truncate();
        IPlot plot = pw.getPlot(location);
        if(plot == null || !plot.hasOwnerPermissions(pl)) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        if(player.equals(context.getSource().getPlayerOrException())) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_self_target");
            return 0;
        }

        if(plot.isDenied(pl.getUUID())) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_deny_target");
            return 0;
        }

        plot.denyPlayer(player.getUUID());
        LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.deny.success");

        return 1;
    }

    private int executeAllow(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(player);

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = pl.getLocation().truncate();
        IPlot plot = pw.getPlot(location);
        if(plot == null || !plot.hasOwnerPermissions(pl)) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        if(player.equals(context.getSource().getPlayerOrException())) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_self_target");
            return 0;
        }

        if(!plot.isDenied(pl.getUUID())) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_undeny_target");
            return 0;
        }

        plot.undenyPlayer(player.getUUID());
        LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.allow.success");

        return 1;
    }

    private int executeMerge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        ServerPlayer spl = context.getSource().getPlayerOrException();
        MPlayer pl = FabricPlayer.wrap(spl);

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = pl.getLocation().truncate();
        PlotPos pos = PlotPos.fromCoords(location.getX(), location.getZ(), pw.getPlotSize(), pw.getRoadSize());

        if(pos == null) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_location");
            return 0;
        }

        IPlot plot = pw.getPlotRegistry().getPlotAt(pos);
        if(plot == null || !pl.getUUID().equals(plot.getOwner())) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        PlotDirection dir = PlotDirection.byName(spl.getDirection().getName());
        if(dir == null) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_direction");
            return 0;
        }

        PlotPos otherPos = pos.getAdjacent(dir);
        IPlot other = pw.getPlotRegistry().getPlotAt(otherPos);

        if(other == null || !other.getOwner().equals(pl.getUUID())) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.claimed");
            return 0;
        }

        int max = CreativePlotsAPI.getInstance().getMaxPlotSize();
        if(plot.getPositions().size() + other.getPositions().size() > max) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.too_big", new CustomPlaceholderInline("limit", max+""));
            return 0;
        }


        plot.merge(other);
        ((Plot) plot).register(pw.getPlotRegistry());

        LangModule.sendCommandSuccess(context, CreativePlotsAPI.getInstance().getLangProvider(), false, "command.merge.success");

        ServerLevel l = context.getSource().getLevel();

        ChunkGenerator gen = l.getChunkSource().getGenerator();
        if (gen instanceof PlotworldGenerator) {

            int height = pw.getGenerationHeight();
            BlockPos.MutableBlockPos bpos = new BlockPos.MutableBlockPos();
            BlockState border = Registry.BLOCK.get(ConversionUtil.toResourceLocation(pw.getClaimedBorderBlock())).defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockState top = ((PlotworldGenerator) gen).settings().getBlockForLayer(pw.getGenerationHeight());

            List<Region> roads = new ArrayList<>();

            int plotSize = pw.getPlotSize();
            int roadSize = pw.getRoadSize();
            int halfSize = plotSize / 2;

            // Roads
            for(PlotPos p : new PlotPos[] { pos, otherPos } ) {

                Vec3i loc = pw.toLocation(p).add(halfSize);

                for (PlotDirection d : PlotDirection.values()) {

                    if(!plot.getPositions().contains(p.getAdjacent(d))) continue;

                    Vec3i r0 = new Vec3i(
                            loc.getX() + (halfSize * d.getXShift()),
                            0,
                            loc.getZ() + (halfSize * d.getZShift())
                    );

                    Vec3i r1 = new Vec3i(
                            r0.getX() - (halfSize * d.getZShift()),
                            0,
                            r0.getZ() - (halfSize * d.getXShift())
                    );

                    Vec3i r2 = r1.add(new Vec3i(
                            (plotSize * d.getZShift()) + (roadSize * d.getXShift()),
                            0,
                            (plotSize * d.getXShift()) + (roadSize * d.getZShift()))
                    );

                    Region crossed = Region.fromPoints(r1, r2);
                    roads.add(crossed);
                }

                // Corners
                boolean[] corners = { true, true, true, true };
                for(int x = -1 ; x <= 1 ; x++) {
                    for(int z = -1 ; z <= 1 ; z++) {

                        if(x == 0 && z == 0) continue;

                        PlotPos surrounding = new PlotPos(p.getX() + x, p.getZ() + z);
                        if(!plot.getPositions().contains(surrounding)) {
                            if(x <= 0 && z <= 0) {
                                corners[0] = false;
                            }
                            if(x >= 0 && z <= 0) {
                                corners[1] = false;
                            }
                            if(x <= 0 && z >= 0) {
                                corners[2] = false;
                            }
                            if(x >= 0 && z >= 0) {
                                corners[3] = false;
                            }
                        }
                    }
                }

                for(int i = 0 ; i < 4 ; i++) {

                    if(!corners[i]) continue;

                    int shiftX = i % 2 == 0 ? -1 : 1;
                    int shiftZ = i > 2 ? 1 : -1;

                    Vec3i c1 = loc.add(new Vec3i(shiftX * halfSize, 0, shiftZ * halfSize));
                    Vec3i c2 = new Vec3i(shiftX * roadSize, 0, shiftZ * roadSize);

                    Region corner = Region.normalized(c1,c2);
                    roads.add(corner);
                }

            }

            // Remove roads
            for(Region road : roads) {

                for (int x = road.getLowerBound().getX(); x < road.getUpperBound().getX(); x++) {
                    for (int z = road.getLowerBound().getZ(); z < road.getUpperBound().getZ(); z++) {

                        bpos.set(x, height, z);
                        l.setBlock(bpos, top, 2);
                        l.blockUpdated(bpos, top.getBlock());

                        bpos.set(x, height + 1, z);
                        l.setBlock(bpos, air, 2);
                        l.blockUpdated(bpos, air.getBlock());
                    }
                }
            }

            // Fix Borders
            for(Region reg : plot.getArea()) {

                Region newReg = reg.outset(1);
                for(int x = newReg.getLowerBound().getX() ; x <= newReg.getUpperBound().getX() ; x++) {
                    for(int z = newReg.getLowerBound().getZ() ; z <= newReg.getUpperBound().getZ() ; z++) {

                        if(plot.contains(new Vec3i(x,height,z))) continue;

                        bpos.set(x, height + 1, z);
                        l.setBlock(bpos, border, 2);
                        l.blockUpdated(bpos, border.getBlock());
                    }
                }

            }
        }

        return 1;
    }


}
