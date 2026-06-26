package com.example.dimensionbleed;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DimensionBleedMod implements ModInitializer {
    public static final String MOD_ID = "dimension_bleed";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final long CHUNK_SEED_SALT = 0x6D2B_79F5_AA66_7B1DL;
    private static final long DIMENSION_SALT = 0xC2B2_AE3D_27D4_EB4FL;

    @Override
    public void onInitialize() {
        ServerChunkEvents.CHUNK_LOAD.register(DimensionBleedMod::onChunkLoad);
        LOGGER.info("Dimension Bleed initialized: every chunk receives a unique deterministic seed and a chaotic dimension palette.");
    }

    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        long chunkSeed = createChunkSeed(world, chunk.getPos());
        Random random = new CheckedRandom(chunkSeed);

        BleedPalette palette = BleedPalette.forWorld(world.getRegistryKey(), random);
        applyChunkRewrite(world, chunk, random, palette);
    }

    public static long createChunkSeed(ServerWorld world, ChunkPos pos) {
        long seed = world.getSeed() ^ CHUNK_SEED_SALT;
        seed = mix(seed + pos.x * 0x9E37_79B9_7F4A_7C15L);
        seed = mix(seed + pos.z * 0xBF58_476D_1CE4_E5B9L);
        seed = mix(seed ^ world.getRegistryKey().getValue().toString().hashCode() * DIMENSION_SALT);
        return seed;
    }

    private static void applyChunkRewrite(ServerWorld world, WorldChunk chunk, Random random, BleedPalette palette) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        int crustDepth = 2 + random.nextInt(4);

        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunkPos.getStartX() + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunkPos.getStartZ() + localZ;
                int topY = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, worldX, worldZ) - 1;
                if (topY <= world.getBottomY()) {
                    continue;
                }

                int columnDepth = crustDepth + random.nextInt(3);
                for (int depth = 0; depth < columnDepth; depth++) {
                    int y = topY - depth;
                    if (y <= world.getBottomY()) {
                        break;
                    }

                    mutable.set(worldX, y, worldZ);
                    if (!chunk.getBlockState(mutable).isAir() && !chunk.getBlockState(mutable).isOf(Blocks.BEDROCK)) {
                        Block block = depth == 0 ? palette.pickSurface(random) : palette.pickUnderground(random);
                        chunk.setBlockState(mutable, block.getDefaultState(), false);
                    }
                }
            }
        }
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58_476D_1CE4_E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D0_49BB_1331_11EBL;
        return value ^ (value >>> 31);
    }

    private enum BleedPalette {
        NETHER(
                new Block[] { Blocks.NETHERRACK, Blocks.CRIMSON_NYLIUM, Blocks.WARPED_NYLIUM, Blocks.SOUL_SAND },
                new Block[] { Blocks.NETHERRACK, Blocks.BLACKSTONE, Blocks.BASALT, Blocks.SOUL_SOIL }
        ),
        END(
                new Block[] { Blocks.END_STONE, Blocks.PURPUR_BLOCK, Blocks.OBSIDIAN },
                new Block[] { Blocks.END_STONE, Blocks.OBSIDIAN, Blocks.PURPUR_BLOCK }
        );

        private final Block[] surfaceBlocks;
        private final Block[] undergroundBlocks;

        BleedPalette(Block[] surfaceBlocks, Block[] undergroundBlocks) {
            this.surfaceBlocks = surfaceBlocks;
            this.undergroundBlocks = undergroundBlocks;
        }

        private Block pickSurface(Random random) {
            return surfaceBlocks[random.nextInt(surfaceBlocks.length)];
        }

        private Block pickUnderground(Random random) {
            return undergroundBlocks[random.nextInt(undergroundBlocks.length)];
        }

        private static BleedPalette forWorld(RegistryKey<World> worldKey, Random random) {
            if (worldKey == World.NETHER) {
                return END;
            }
            if (worldKey == World.END) {
                return NETHER;
            }
            return random.nextBoolean() ? NETHER : END;
        }
    }
}
