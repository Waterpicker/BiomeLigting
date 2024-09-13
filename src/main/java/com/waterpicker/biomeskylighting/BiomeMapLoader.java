package com.waterpicker.biomeskylighting;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BiomeMapLoader implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final BiomeMapLoader INSTANCE = new BiomeMapLoader();
    private final Map<ResourceLocation, Integer> biomeMap = new HashMap<>();
    private static FileToIdConverter biomeFileLighting = new FileToIdConverter("biomelights", "biomelights.json");
    private static Gson GSON = new Gson();

    private BiomeMapLoader() {
    }

    public static BiomeMapLoader getInstance() {
        return INSTANCE;
    }

    public Map<ResourceLocation, Integer> getBiomeMap() {
        return biomeMap;
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        biomeMap.clear();
        biomeFileLighting.listMatchingResourceStacks(manager).forEach((name, list) -> {
            list.forEach((resource -> {
                try {
                    var obj = resource.openAsReader();
                    var data = BiomeSkyLightingData.CODEC.decode(JsonOps.INSTANCE, GSON.fromJson(obj, JsonObject.class)).getOrThrow(false, s -> {
                    }).getFirst();
                    if (!data.replace()) {
                        biomeMap.clear();
                    }

                    biomeMap.putAll(data.biomelights());
                } catch (IOException ignore) {}
            }));
        });

        System.out.println("Derp: " + biomeMap);
    }

    public OptionalInt getLightValue(ResourceLocation block) {
        if (biomeMap.containsKey(block)) {
            return OptionalInt.of(biomeMap.get(block));
        }
        return OptionalInt.empty();
    }
}