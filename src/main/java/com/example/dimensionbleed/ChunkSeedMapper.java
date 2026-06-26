package com.example.dimensionbleed;

import net.minecraft.util.math.ChunkPos;

public final class ChunkSeedMapper {
    private static final long CHUNK_SEED_SALT = 0x6D2B_79F5_AA66_7B1DL;
    private static final int MAX_OFFSET_CHUNKS = 1_000_000;

    private ChunkSeedMapper() {
    }

    public static long createChunkSeed(ChunkPos pos) {
        long seed = CHUNK_SEED_SALT;
        seed = mix(seed + pos.x * 0x9E37_79B9_7F4A_7C15L);
        seed = mix(seed + pos.z * 0xBF58_476D_1CE4_E5B9L);
        return seed;
    }

    public static ChunkPos remapToSeededChunk(ChunkPos originalPos) {
        long seed = createChunkSeed(originalPos);
        int offsetX = boundedOffset(seed);
        int offsetZ = boundedOffset(Long.rotateLeft(seed, 32));
        return new ChunkPos(originalPos.x + offsetX, originalPos.z + offsetZ);
    }

    private static int boundedOffset(long seed) {
        int positive = (int) (mix(seed) & 0x7FFF_FFFFL);
        return positive % (MAX_OFFSET_CHUNKS * 2 + 1) - MAX_OFFSET_CHUNKS;
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58_476D_1CE4_E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D0_49BB_1331_11EBL;
        return value ^ (value >>> 31);
    }
}
