package com.example.dimensionbleed;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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
    private static final int BLEED_CHANCE = 80;

    @Override
    public void onInitialize() {
        ServerChunkEvents.CHUNK_LOAD.register(DimensionBleedMod::onChunkLoad);
        LOGGER.info("Dimension Bleed initialized: chunks receive unique deterministic seeds; rare dimension bleed is enabled.");
    }

    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        long chunkSeed = createChunkSeed(world, chunk.getPos());
        Random random = new CheckedRandom(chunkSeed);

        if (random.nextInt(BLEED_CHANCE) != 0) {
            return;
        }

        BleedPalette palette = BleedPalette.forWorld(world.getRegistryKey(), random);
        applyBleedPatch(world, chunk, random, palette);
    }

    public static long createChunkSeed(ServerWorld world, ChunkPos pos) {
        long seed = world.getSeed() ^ CHUNK_SEED_SALT;
        seed = mix(seed + pos.x * 0x9E37_79B9_7F4A_7C15L);
        seed = mix(seed + pos.z * 0xBF58_476D_1CE4_E5B9L);
        seed = mix(seed ^ world.getRegistryKey().getValue().toString().hashCode() * DIMENSION_SALT);
        return seed;
    }

    private static void applyBleedPatch(ServerWorld world, WorldChunk chunk, Random random, BleedPalette palette) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        int centerX = chunkPos.getStartX() + 4 + random.nextInt(8);
        int centerZ = chunkPos.getStartZ() + 4 + random.nextInt(8);
        int radius = 4 + random.nextInt(4);
        int radiusSquared = radius * radius;

        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunkPos.getStartX() + localX;
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunkPos.getStartZ() + localZ;
                int dx = worldX - centerX;
                int dz = worldZ - centerZ;
                if (dx * dx + dz * dz > radiusSquared + random.nextInt(5)) {
                    continue;
                }

                int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, worldX, worldZ) - 1;
                if (topY <= world.getBottomY()) {
                    continue;
                }

                mutable.set(worldX, topY, worldZ);
                BlockState current = world.getBlockState(mutable);
                if (current.isAir() || current.isOf(Blocks.BEDROCK)) {
                    continue;
                }

                Block block = palette.pick(random);
                world.setBlockState(mutable, block.getDefaultState(), Block.NOTIFY_LISTENERS);

                if (palette == BleedPalette.NETHER && random.nextInt(5) == 0 && topY + 1 < world.getTopY()) {
                    mutable.set(worldX, topY + 1, worldZ);
                    world.setBlockState(mutable, Blocks.FIRE.getDefaultState(), Block.NOTIFY_LISTENERS);
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
        NETHER(new Block[] { Blocks.NETHERRACK, Blocks.BLACKSTONE, Blocks.BASALT, Blocks.SOUL_SAND }),
        END(new Block[] { Blocks.END_STONE, Blocks.PURPUR_BLOCK, Blocks.OBSIDIAN });

        private final Block[] blocks;

        BleedPalette(Block[] blocks) {
            this.blocks = blocks;
        }

        private Block pick(Random random) {
            return blocks[random.nextInt(blocks.length)];
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
