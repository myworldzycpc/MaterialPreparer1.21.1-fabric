package io.github.myworldzycpc.material_preparer;

import net.fabricmc.api.ModInitializer;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MaterialPreparer implements ModInitializer {
    public static final String MOD_ID = "material-preparer";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
