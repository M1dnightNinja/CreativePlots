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
import me.m1dnightninja.midnightcore.api.inventory.AbstractInventoryGUI;
import me.m1dnightninja.midnightcore.api.inventory.MItemStack;
import me.m1dnightninja.midnightcore.api.math.Color;
import me.m1dnightninja.midnightcore.api.math.Vec3d;
import me.m1dnightninja.midnightcore.api.math.Vec3i;
import me.m1dnightninja.midnightcore.api.module.lang.CustomPlaceholderInline;
import me.m1dnightninja.midnightcore.api.player.MPlayer;
import me.m1dnightninja.midnightcore.api.text.MComponent;
import me.m1dnightninja.midnightcore.api.text.MStyle;
import me.m1dnightninja.midnightcore.fabric.inventory.InventoryGUI;
import me.m1dnightninja.midnightcore.fabric.module.lang.LangModule;
import me.m1dnightninja.midnightcore.fabric.player.FabricPlayer;
import me.m1dnightninja.midnightcore.fabric.util.ConversionUtil;
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
                .then(Commands.literal("claim")
                    .executes(context -> executeClaim(context, context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeClaim(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                    )
                )
                .then(Commands.literal("delete")
                    .executes(this::executeDelete)
                )
                .then(Commands.literal("tp")
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
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(context -> executeRename(context, context.getArgument("name", String.class)))
                    )
                )
                .then(Commands.literal("trust")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeTrust(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                    )
                )
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeRemove(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                    )
                )
                .then(Commands.literal("deny")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeDeny(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                    )
                )
                .then(Commands.literal("allow")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeAllow(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                    )
                )
                .then(Commands.literal("merge")
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

        Region reg = pos.toRegion(pw);
        BlockPos.MutableBlockPos bpos = new BlockPos.MutableBlockPos();

        ServerLevel l = context.getSource().getLevel();
        BlockState state = Registry.BLOCK.get(ConversionUtil.toResourceLocation(pw.getClaimedBorderBlock())).defaultBlockState();
        for (int x = reg.getLowerBound().getX() - 1; x < reg.getUpperBound().getX() + 1; x++) {
            for (int z = reg.getLowerBound().getZ() - 1; z < reg.getUpperBound().getZ() + 1; z++) {

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

        if(plot == null || !plot.getOwner().equals(pl.getUUID())) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        InventoryGUI confirm = new InventoryGUI(CreativePlotsAPI.getInstance().getLangProvider().getMessage("command.delete.gui.title"));
        confirm.setItem(MItemStack.Builder.woolWithColor(Color.fromRGBI(10)).withName(CreativePlotsAPI.getInstance().getLangProvider().getMessage("command.delete.gui.yes")).build(), 3, (type, user) -> {
            doDelete(pw, plot, context.getSource().getLevel());
            LangModule.sendCommandSuccess(context, CreativePlotsAPI.getInstance().getLangProvider(), false, "command.delete.success");
            InventoryGUI.closeMenu(user);
        });
        confirm.setItem(MItemStack.Builder.woolWithColor(Color.fromRGBI(12)).withName(CreativePlotsAPI.getInstance().getLangProvider().getMessage("command.delete.gui.no")).build(), 5, (type, user) -> {
            InventoryGUI.closeMenu(user);
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
        if(plot == null || !plot.getOwner().equals(pl.getUUID())) {
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

        if(plot == null || !plot.getOwner().equals(pl.getUUID())) {
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

        if(plot == null || !plot.getOwner().equals(pl.getUUID())) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        if(player.equals(context.getSource().getPlayerOrException())) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_target");
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
        if(plot == null || !plot.getOwner().equals(pl.getUUID())) {
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
        if(plot == null || !plot.getOwner().equals(pl.getUUID())) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        if(player.equals(context.getSource().getPlayerOrException())) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_target");
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
        if(plot == null || !plot.getOwner().equals(pl.getUUID())) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        if(player.equals(context.getSource().getPlayerOrException())) {
            LangModule.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_target");
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
        if(plot == null || !plot.getOwner().equals(pl.getUUID())) {
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
        if(plot.getPositions().size() >= max || other.getPositions().size() >= max) {
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

            // Roads
            for(PlotPos p : new PlotPos[] { pos, otherPos } ) {
                for (PlotDirection d : PlotDirection.values()) {

                    if(!plot.getPositions().contains(p.getAdjacent(d))) continue;

                    Vec3i loc = pw.toLocation(p);
                    Region crossed = Region.normalized(
                        new Vec3i(loc.getX() + (pw.getPlotSize() * d.getXShift()), pw.getGenerationHeight(), loc.getZ() + (pw.getPlotSize() * d.getZShift())),
                        new Vec3i(pw.getPlotSize() - ((pw.getPlotSize() - pw.getRoadSize()) * d.getXShift()), 0, pw.getPlotSize() - ((pw.getPlotSize() - pw.getRoadSize()) * d.getZShift()))
                    );
                    roads.add(crossed);

                    // Corners
                    boolean[] corners = { true, true, true, true };
                    for(int x = -1 ; x <= 1 ; x++) {
                        for(int z = -1 ; z <= 1 ; z++) {
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

                        int shiftX = i % 2 == 0 ? 0 : 1;
                        int shiftZ = i > 2 ? 1 : 0;

                        Region corner = Region.normalized(
                                new Vec3i(loc.getX() + (shiftX * pw.getPlotSize()), pw.getGenerationHeight(), loc.getZ() + (shiftZ * pw.getPlotSize())),
                                new Vec3i(pw.getRoadSize(), 0, pw.getRoadSize())
                        );
                        roads.add(corner);
                    }

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
                for(int x = newReg.getLowerBound().getX() ; x < newReg.getUpperBound().getX() + 1 ; x++) {
                    for(int z = newReg.getLowerBound().getZ() ; z < newReg.getUpperBound().getZ() + 1 ; z++) {

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
