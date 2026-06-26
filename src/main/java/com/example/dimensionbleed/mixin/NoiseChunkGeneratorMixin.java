package com.example.dimensionbleed.mixin;

import com.example.dimensionbleed.ChunkSeedMapper;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NoiseChunkGenerator.class)
public abstract class NoiseChunkGeneratorMixin {
    @Redirect(
            method = "populateNoise",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;getPos()Lnet/minecraft/util/math/ChunkPos;"),
            require = 0
    )
    private ChunkPos dimensionBleed$remapNoiseChunkPos(Chunk chunk) {
        return ChunkSeedMapper.remapToSeededChunk(chunk.getPos());
    }

    @Redirect(
            method = "buildSurface",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;getPos()Lnet/minecraft/util/math/ChunkPos;"),
            require = 0
    )
    private ChunkPos dimensionBleed$remapSurfaceChunkPos(Chunk chunk) {
        return ChunkSeedMapper.remapToSeededChunk(chunk.getPos());
    }
}
