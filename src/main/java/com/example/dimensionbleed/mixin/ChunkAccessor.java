package com.example.dimensionbleed.mixin;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Chunk.class)
public interface ChunkAccessor {
    @Mutable
    @Accessor("pos")
    void dimensionBleed$setPos(ChunkPos pos);
}
