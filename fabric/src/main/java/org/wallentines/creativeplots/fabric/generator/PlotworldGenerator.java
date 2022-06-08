package org.wallentines.creativeplots.fabric.generator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.wallentines.creativeplots.api.math.Region;
import org.wallentines.creativeplots.api.plot.PlotPos;
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
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.wallentines.midnightcore.api.MidnightCoreAPI;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@ParametersAreNonnullByDefault
public class PlotworldGenerator extends ChunkGenerator {

    public static final Codec<PlotworldGenerator> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            PlotworldGeneratorSettings.CODEC.fieldOf("settings").forGetter(generator -> generator.plotworld)
        ).apply(instance, instance.stable(PlotworldGenerator::new)));

    private final PlotworldGeneratorSettings plotworld;

    public PlotworldGenerator(PlotworldGeneratorSettings world) {
        super(new MappedRegistry<>(Registry.STRUCTURE_SET_REGISTRY, Lifecycle.stable(), (t) -> null), Optional.empty(), new FixedBiomeSource(world.biome));
        this.plotworld = world;
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel worldGenLevel, ChunkAccess chunkAccess, StructureManager structureFeatureManager) { }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion worldGenRegion, long l, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunkAccess, GenerationStep.Carving carving) { }

    @Override
    public void buildSurface(WorldGenRegion worldGenRegion, StructureManager structureFeatureManager, RandomState randomState, ChunkAccess chunkAccess) {

        int height = plotworld.getGenerationHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap hm1 = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap hm2 = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        BlockState roadState = plotworld.getRoadBlock();
        BlockState borderState = plotworld.getBorderBlock();

        int ox = chunkAccess.getPos().getMinBlockX();
        int oz = chunkAccess.getPos().getMinBlockZ();

        for(int y = worldGenRegion.getMinBuildHeight() ; y <= height + 1; y++) {

            BlockState state = plotworld.getBlockForLayer(y, worldGenRegion.getMinBuildHeight());

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

    public PlotworldGeneratorSettings settings() {
        return plotworld;
    }

    public void regenerateRegion(Region r, Level l) {

        MidnightCoreAPI.getLogger().warn("regenerating...");

        int height = plotworld.getGenerationHeight();
        BlockState roadState = plotworld.getRoadBlock();
        BlockState borderState = plotworld.getBorderBlock();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for(int y = r.getLowerBound().getY() ; y < r.getUpperBound().getY() + 1 ; y++) {

            if(y > l.getHeight()) break;
            BlockState state = plotworld.getBlockForLayer(y, l.getMinBuildHeight());

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
    public int getSpawnHeight(LevelHeightAccessor levelHeightAccessor) {
        return plotworld.getGenerationHeight() + 1;
    }

    @Override
    public int getGenDepth() {
        return 0;
    }


    @Override
    public int getBaseHeight(int i, int j, Heightmap.Types types, LevelHeightAccessor acc, RandomState state) {

        return plotworld.getGenerationHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int i, int j, LevelHeightAccessor acc, RandomState state) {

        int height = plotworld.getGenerationHeight();
        BlockState[] states = new BlockState[height];

        boolean road = PlotPos.fromCoords(i, j, plotworld.getPlotSize(), plotworld.getRoadSize()) == null;

        for(int y = acc.getMinBuildHeight() ; y < height ; y++) {
            BlockState blk = plotworld.getBlockForLayer(y+1, acc.getMinBuildHeight());
            if(road && y + 1 == height) {
                blk = plotworld.getRoadBlock();
            }
            states[y] = blk;
        }

        return new NoiseColumn(acc.getMinBuildHeight(), states);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {}

    @Override
    public void createStructures(RegistryAccess registryAccess, RandomState state, StructureManager structureFeatureManager, ChunkAccess chunkAccess, StructureTemplateManager structureManager, long l) { }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, StructureManager structureFeatureManager, ChunkAccess chunkAccess) {
        return CompletableFuture.completedFuture(chunkAccess);
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getFirstFreeHeight(int i, int j, Heightmap.Types types, LevelHeightAccessor acc, RandomState randomState) {
        return acc.getMinBuildHeight();
    }

    @Override
    public int getFirstOccupiedHeight(int i, int j, Heightmap.Types types, LevelHeightAccessor acc, RandomState randomState) {
        return acc.getMinBuildHeight();
    }


    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos blockPos) { }


    public static class PlotworldGeneratorSettings {

        public static final Codec<PlotworldGeneratorSettings> CODEC = RecordCodecBuilder.create((instance) ->
                instance.group(
                        Codec.INT.fieldOf("plot_size").forGetter(PlotworldGeneratorSettings::getPlotSize),
                        Codec.INT.fieldOf("road_size").forGetter(PlotworldGeneratorSettings::getRoadSize),
                        Codec.INT.fieldOf("generation_height").forGetter(PlotworldGeneratorSettings::getGenerationHeight),
                        Biome.CODEC.fieldOf("biome_id").forGetter(PlotworldGeneratorSettings::getBiome),
                        Registry.BLOCK.byNameCodec().fieldOf("road_block").forGetter(settings -> settings.getRoadBlock().getBlock()),
                        Registry.BLOCK.byNameCodec().fieldOf("border_block").forGetter(settings -> settings.getBorderBlock().getBlock()),
                        FlatLayerInfo.CODEC.listOf().fieldOf("layers").forGetter(PlotworldGeneratorSettings::getLayers)
                ).apply(instance, instance.stable(PlotworldGeneratorSettings::new)));

        private final int roadSize;
        private final int plotSize;
        private final int generationHeight;
        private final Holder<Biome> biome;
        private final List<FlatLayerInfo> layers;
        private final BlockState roadBlock;
        private final BlockState borderBlock;

        public PlotworldGeneratorSettings(int plotSize, int roadSize, int generationHeight, Holder<Biome> biome, Block roadBlock, Block borderBlock, List<FlatLayerInfo> layers) {
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

        public Holder<Biome> getBiome() {
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
        public BlockState getBlockForLayer(int y, int minBuildHeight) {

            int index = y - minBuildHeight;
            for(FlatLayerInfo inf : layers) {
                index -= inf.getHeight();
                if(index <= 0) return inf.getBlockState();
            }

            return Blocks.AIR.defaultBlockState();
        }
    }

}
