package io.github.myworldzycpc.material_preparer.client;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MaterialPreparerClient implements ClientModInitializer {
    public static Map<BlockPos, List<ItemStack>> chestMap = new HashMap<>();
    public static Set<BlockPos> craftingTables = new HashSet<>();

    public static Minecraft mc = Minecraft.getInstance();

    public static void showMessage(Component component) {
        if (mc.player != null) {
            mc.player.sendSystemMessage(component);
        }
    }

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.
        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register(((blockEntity, world) -> {
            showMessage(Component.literal(blockEntity.getBlockPos().toString()));
            chestMap.put(blockEntity.getBlockPos(), new ArrayList<>());
        }));
    }
}