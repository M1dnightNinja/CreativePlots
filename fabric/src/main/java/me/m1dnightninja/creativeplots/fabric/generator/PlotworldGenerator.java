package me.m1dnightninja.creativeplots.fabric.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.m1dnightninja.creativeplots.api.math.Region;
import me.m1dnightninja.creativeplots.api.plot.PlotPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class PlotworldGenerator extends ChunkGenerator {

    public static final Codec<PlotworldGenerator> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            PlotworldGeneratorSettings.CODEC.fieldOf("settings").forGetter(generator -> generator.plotworld)
        ).apply(instance, instance.stable(PlotworldGenerator::new)));

    private final PlotworldGeneratorSettings plotworld;

    public PlotworldGenerator(PlotworldGeneratorSettings world) {
        super(new FixedBiomeSource(world.biome), new StructureSettings(false));
        this.plotworld = world;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long l) {
        return this;
    }

    public PlotworldGeneratorSettings settings() {
        return plotworld;
    }

    public void regenerateRegion(Region r, Level l) {

        int height = plotworld.getGenerationHeight();
        BlockState roadState = plotworld.getRoadBlock();
        BlockState borderState = plotworld.getBorderBlock();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for(int y = r.getLowerBound().getY() ; y < r.getUpperBound().getY() + 1 ; y++) {

            if(y > l.getHeight()) break;
            BlockState state = plotworld.getBlockForLayer(y);

            for(int x = r.getLowerBound().getX() ; x < r.getUpperBound().getX() + 1; x++) {
                for(int z = r.getLowerBound().getZ() ; z < r.getUpperBound().getZ() + 1 ; z++) {

                    BlockState tState = state;
                    pos.set(x,y,z);
                    if(y == height) {

                        if(PlotPos.fromCoords(x, z, plotworld.getPlotSize(), plotworld.getRoadSize()) == null) {
                            tState = roadState;
                        }

                    } else if(y == height + 1) {

                        if(PlotPos.isPlotBorder(x, z, plotworld.getPlotSize(), plotworld.getRoadSize())) {
                            tState = borderState;
                        }

                    }

                    l.setBlock(pos, tState, 2);
                }
            }
        }

    }

    @Override
    public void buildSurfaceAndBedrock(WorldGenRegion worldGenRegion, ChunkAccess chunkAccess) {

        int height = plotworld.getGenerationHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap hm1 = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap hm2 = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        BlockState roadState = plotworld.getRoadBlock();
        BlockState borderState = plotworld.getBorderBlock();

        int ox = chunkAccess.getPos().getMinBlockX();
        int oz = chunkAccess.getPos().getMinBlockZ();

        for(int y = worldGenRegion.getMinBuildHeight() ; y <= height + 1; y++) {

            BlockState state = plotworld.getBlockForLayer(y);

            for(int x = 0 ; x < 16 ; x++) {
                for(int z = 0 ; z < 16 ; z++) {

                    BlockState tState = state;
                    pos.set(x,y,z);
                    if(y == height) {

                        int px = x + ox;
                        int pz = z + oz;

                        if(PlotPos.fromCoords(px, pz, plotworld.getPlotSize(), plotworld.getRoadSize()) == null) {
                            tState = roadState;
                        }

                    } else if(y == height + 1) {

                        int px = x + ox;
                        int pz = z + oz;

                        if(PlotPos.isPlotBorder(px, pz, plotworld.getPlotSize(), plotworld.getRoadSize())) {
                            tState = borderState;
                        }

                    }

                    chunkAccess.setBlockState(pos, tState, false);
                    hm1.update(x,y,z,state);
                    hm2.update(x,y,z,state);

                }
            }
        }

    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor levelAccessor, StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess) {

        return CompletableFuture.completedFuture(chunkAccess);
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor levelHeightAccessor) {
        return plotworld.getGenerationHeight() + 1;
    }

    @Override
    public int getBaseHeight(int i, int j, Heightmap.Types types, LevelHeightAccessor acc) {

        return plotworld.getGenerationHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int i, int j, LevelHeightAccessor acc) {

        int height = plotworld.getGenerationHeight();
        BlockState[] states = new BlockState[height];

        boolean road = PlotPos.fromCoords(i, j, plotworld.getPlotSize(), plotworld.getRoadSize()) == null;

        for(int y = acc.getMinBuildHeight() ; y < height ; y++) {
            BlockState blk = plotworld.getBlockForLayer(y+1);
            if(road && y + 1 == height) {
                blk = plotworld.getRoadBlock();
            }
            states[y] = blk;
        }

        return new NoiseColumn(acc.getMinBuildHeight(), states);
    }

    @Override
    public void applyCarvers(long l, BiomeManager biomeManager, ChunkAccess chunkAccess, GenerationStep.Carving carving) { }

    @Override
    public void applyBiomeDecoration(WorldGenRegion worldGenRegion, StructureFeatureManager structureFeatureManager) { }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {}

    @Override
    public void createStructures(RegistryAccess registryAccess, StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess, StructureManager structureManager, long l) { }

    @Nullable
    @Override
    public BlockPos findNearestMapFeature(ServerLevel serverLevel, StructureFeature<?> structureFeature, BlockPos blockPos, int i, boolean bl) {
        return null;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public boolean hasStronghold(ChunkPos chunkPos) {
        return false;
    }

    @Override
    public int getFirstFreeHeight(int i, int j, Heightmap.Types types, LevelHeightAccessor acc) {
        return acc.getMinBuildHeight();
    }

    @Override
    public int getFirstOccupiedHeight(int i, int j, Heightmap.Types types, LevelHeightAccessor acc) {
        return acc.getMinBuildHeight();
    }


    public static class PlotworldGeneratorSettings {

        public static final Codec<PlotworldGeneratorSettings> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(
                        Codec.INT.fieldOf("plot_size").forGetter(PlotworldGeneratorSettings::getPlotSize),
                        Codec.INT.fieldOf("road_size").forGetter(PlotworldGeneratorSettings::getRoadSize),
                        Codec.INT.fieldOf("generation_height").forGetter(PlotworldGeneratorSettings::getGenerationHeight),
                        Biome.CODEC.fieldOf("biome_id").forGetter(PlotworldGeneratorSettings::getBiome),
                        Registry.BLOCK.fieldOf("road_block").forGetter(settings -> settings.getRoadBlock().getBlock()),
                        Registry.BLOCK.fieldOf("border_block").forGetter(settings -> settings.getBorderBlock().getBlock()),
                        FlatLayerInfo.CODEC.listOf().fieldOf("layers").forGetter(PlotworldGeneratorSettings::getLayers)
                ).apply(instance, instance.stable(PlotworldGeneratorSettings::new)));

        private final int roadSize;
        private final int plotSize;
        private final int generationHeight;
        private final Supplier<Biome> biome;
        private final List<FlatLayerInfo> layers;
        private final BlockState roadBlock;
        private final BlockState borderBlock;

        public PlotworldGeneratorSettings(int plotSize, int roadSize, int generationHeight, Supplier<Biome> biome, Block roadBlock, Block borderBlock, List<FlatLayerInfo> layers) {
            this.plotSize = plotSize;
            this.roadSize = roadSize;
            this.generationHeight = generationHeight;
            this.biome = biome;
            this.roadBlock = roadBlock.defaultBlockState();
            this.borderBlock = borderBlock.defaultBlockState();
            this.layers = layers;
        }

        public int getRoadSize() {
            return roadSize;
        }

        public int getPlotSize() {
            return plotSize;
        }

        public Supplier<Biome> getBiome() {
            return biome;
        }

        public int getGenerationHeight() {
            return generationHeight;
        }

        public List<FlatLayerInfo> getLayers() {
            return layers;
        }

        public BlockState getRoadBlock() {
            return roadBlock;
        }

        public BlockState getBorderBlock() {
            return borderBlock;
        }
        public BlockState getBlockForLayer(int y) {

            int index = y;
            for(FlatLayerInfo inf : layers) {
                index -= inf.getHeight();
                if(index <= 0) return inf.getBlockState();
            }

            return Blocks.AIR.defaultBlockState();
        }
    }

}
