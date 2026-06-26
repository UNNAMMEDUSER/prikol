package com.example.dimensionbleed.mixin;

import com.example.dimensionbleed.ChunkSeedMapper;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(NoiseChunkGenerator.class)
public abstract class NoiseChunkGeneratorMixin {
    private static final Map<Chunk, ChunkPos> DIMENSION_BLEED_ORIGINAL_POSITIONS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    @Inject(method = "populateNoise", at = @At("HEAD"))
    private void dimensionBleed$remapNoiseStart(
            Blender blender,
            NoiseConfig noiseConfig,
            StructureAccessor structureAccessor,
            Chunk chunk,
            CallbackInfoReturnable<CompletableFuture<Chunk>> cir
    ) {
        dimensionBleed$swapChunkPos(chunk);
    }

    @Inject(method = "populateNoise", at = @At("RETURN"), cancellable = true)
    private void dimensionBleed$remapNoiseEnd(
            Blender blender,
            NoiseConfig noiseConfig,
            StructureAccessor structureAccessor,
            Chunk chunk,
            CallbackInfoReturnable<CompletableFuture<Chunk>> cir
    ) {
        CompletableFuture<Chunk> originalFuture = cir.getReturnValue();
        cir.setReturnValue(originalFuture.whenComplete((generatedChunk, throwable) -> dimensionBleed$restoreChunkPos(chunk)));
    }

    @Inject(method = "buildSurface", at = @At("HEAD"))
    private void dimensionBleed$remapSurfaceStart(
            StructureWorldAccess world,
            StructureAccessor structureAccessor,
            NoiseConfig noiseConfig,
            Chunk chunk,
            CallbackInfo ci
    ) {
        dimensionBleed$swapChunkPos(chunk);
    }

    @Inject(method = "buildSurface", at = @At("RETURN"))
    private void dimensionBleed$remapSurfaceEnd(
            StructureWorldAccess world,
            StructureAccessor structureAccessor,
            NoiseConfig noiseConfig,
            Chunk chunk,
            CallbackInfo ci
    ) {
        dimensionBleed$restoreChunkPos(chunk);
    }

    private static void dimensionBleed$swapChunkPos(Chunk chunk) {
        ChunkPos originalPos = chunk.getPos();
        if (DIMENSION_BLEED_ORIGINAL_POSITIONS.containsKey(chunk)) {
            return;
        }

        DIMENSION_BLEED_ORIGINAL_POSITIONS.put(chunk, originalPos);
        ((ChunkAccessor) chunk).dimensionBleed$setPos(ChunkSeedMapper.remapToSeededChunk(originalPos));
    }

    private static void dimensionBleed$restoreChunkPos(Chunk chunk) {
        ChunkPos originalPos = DIMENSION_BLEED_ORIGINAL_POSITIONS.remove(chunk);
        if (originalPos != null) {
            ((ChunkAccessor) chunk).dimensionBleed$setPos(originalPos);
        }
    }
}
