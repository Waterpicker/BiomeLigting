package com.waterpicker.biomeskylighting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public abstract class BiomeLightProvider implements DataProvider {
    private static final Logger LOGGER = LogManager.getLogger();

    private DataGenerator generator;

    public BiomeLightProvider(DataGenerator generator) {
        this.generator = generator;
    }

    abstract protected void generateLights(BiConsumer<String, JsonElement> consumer);

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path path = generator.getPackOutput().getOutputFolder();

        BiConsumer<String, JsonElement> consumer = (identifier, json)  -> {
            java.nio.file.Path outputPath = getOutput(path, identifier);

            DataProvider.saveStable(cache, json, outputPath);
        };

        generateLights(consumer);
        return new CompletableFuture<>(); //TODO why you broke completable future
    }

    @Override
    public String getName() {
        return "BiomeLight";
    }

    private static java.nio.file.Path getOutput(Path rootOutput, String modid) {
        return rootOutput.resolve("data/" + modid + "/biomelights/biomelights.json");
    }

    public static class BiomeLightBuilder {
        private boolean replace = true;
        private final Map<ResourceLocation, Integer> biomeLightMap = new HashMap<>();
        private String modid;

        public BiomeLightBuilder(String modid) {
            this.modid = modid;
        }

        public BiomeLightBuilder biomeLight(ResourceLocation biome, int lightLevel) {
            biomeLightMap.put(biome, lightLevel);
            new BiomeSkyLightingRegister().biomeMap = biomeLightMap;
            return this;
        }

        public BiomeLightBuilder biomeLight(ResourceKey<Biome> biome, int lightLevel) {
            return biomeLight(biome.location(), lightLevel);
        }

        public Map<ResourceLocation, Integer> getBiomeLightMap() {
            return biomeLightMap;
        }

        public BiomeLightBuilder replace(boolean replace) {
            this.replace = replace;
            return this;
        }

        protected void run(BiConsumer<String, JsonElement> consumer) {
            consumer.accept(modid, JsonOps.INSTANCE.withEncoder(BiomeSkyLightingData.CODEC).apply(new BiomeSkyLightingData(replace, biomeLightMap)).getOrThrow(true, a -> {}));
        }
    }
}