package com.waterpicker.biomeskylighting;

import com.google.gson.JsonElement;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiConsumer;

@Mod("biomelighting")
public class BiomeSkyLighting {
    private static final Logger logger = LogManager.getLogger();

    public BiomeSkyLighting() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onDataGather);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BiomeSkyLightingConfig.CONFIG_SPEC);
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListener);
        MinecraftForge.EVENT_BUS.addListener(this::onDataGather);
        MinecraftForge.EVENT_BUS.addListener(this::onServerLoad);
    }

    public void onServerLoad(ServerStartingEvent event) {
        BiomeSkyLightingRegister biomeLightingRegister = new BiomeSkyLightingRegister();
        MinecraftForge.EVENT_BUS.post(biomeLightingRegister);
    }

    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(BiomeMapLoader.getInstance());
    }

    public void onDataGather(GatherDataEvent event) {
        event.getGenerator().addProvider(true, new BiomeLightProvider(event.getGenerator()) {
            @Override
            protected void generateLights(BiConsumer<String, JsonElement> consumer) {
                new BiomeLightBuilder("minecraft/biomelights")
                        .biomeLight(Biomes.FOREST, 8)
                        .biomeLight(Biomes.PLAINS, 10)
                        .biomeLight(Biomes.DRIPSTONE_CAVES, 3)
                        .biomeLight(Biomes.LUSH_CAVES, 5)
                        .biomeLight(Biomes.SWAMP, 6)
                        .biomeLight(Biomes.DESERT, 15)
                        .run(consumer);
            }
        });
    }
}
