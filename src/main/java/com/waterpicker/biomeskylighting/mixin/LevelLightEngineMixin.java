package com.waterpicker.biomeskylighting.mixin;

import com.waterpicker.biomeskylighting.BiomeLightingSkyEngine;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.lighting.LightEventListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(LevelLightEngine.class)
abstract public class LevelLightEngineMixin implements LightEventListener {

    @Mutable
    @Final
    @Shadow
    @Nullable
    private LightEngine<?, ?> skyEngine;

    @Inject(at = @At("TAIL"), method = "<init>")
    public void init(LightChunkGetter chunkProvider, boolean hasBlockLight, boolean hasSkyLight, CallbackInfo ci) {
        skyEngine = hasSkyLight ? new BiomeLightingSkyEngine(chunkProvider) : null;
    }
}
