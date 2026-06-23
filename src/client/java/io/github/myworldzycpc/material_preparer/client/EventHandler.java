package io.github.myworldzycpc.material_preparer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.myworldzycpc.material_preparer.client.keybind.KeybindEntry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import net.minecraft.core.registries.BuiltInRegistries;

import io.github.myworldzycpc.material_preparer.client.keybind.CustomKeybind;

import org.lwjgl.glfw.GLFW;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import static io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient.*;
import static io.github.myworldzycpc.material_preparer.client.config.MaterialPreparerConfig.HANDLER;
import static io.github.myworldzycpc.material_preparer.client.ModMenuIntegration.getModConfigScreen;
import static io.github.myworldzycpc.material_preparer.client.keybind.KeybindHelper.withScreenGuard;

public class EventHandler {

    public static void onBlockEntityLoad(BlockEntity blockEntity, ClientLevel world) {
//        showMessage(Component.literal(blockEntity.getBlockPos().toString()));
        BlockPos pos = blockEntity.getBlockPos();
        if (blockEntity instanceof ChestBlockEntity chestBlockEntity) {
//            ChestType chestType = blockState.getValue(ChestBlock.TYPE);
            ChestType chestType = chestBlockEntity.getBlockState().getValue(ChestBlock.TYPE);
            if (chestType == ChestType.SINGLE || chestType == ChestType.RIGHT) {
                chestMap.put(pos, null);
//                showMessage(Component.literal("Found chest at " + pos));
                if (chestType == ChestType.RIGHT) {
                    BlockPos oppositePos = MaterialPreparerClient.getOppositeChestPos(pos, chestBlockEntity.getBlockState().getValue(ChestBlock.FACING), chestType);
                    chestMap.remove(oppositePos);
                }
            }
        }
    }


    public static void onBlockEntityUnload(BlockEntity blockEntity, ClientLevel world) {
        BlockPos pos = blockEntity.getBlockPos();
        if (blockEntity instanceof ChestBlockEntity chestBlockEntity) {
            ChestType chestType = chestBlockEntity.getBlockState().getValue(ChestBlock.TYPE);
            if (chestType == ChestType.SINGLE || chestType == ChestType.RIGHT) {
                chestMap.remove(pos);
//                showMessage(Component.literal("Removed chest at " + pos));
                if (chestType == ChestType.RIGHT) {
                    BlockPos oppositePos = MaterialPreparerClient.getOppositeChestPos(pos, chestBlockEntity.getBlockState().getValue(ChestBlock.FACING), chestType);
                    if (world.getBlockState(oppositePos).getBlock() instanceof ChestBlock) {
                        chestMap.put(oppositePos, null);
                    }
                }
            }
        }
    }

    public static void onChunkLoad(Level world, LevelChunk chunk) {
        // 记录区块内工作台
        ChunkPos chunkPos = chunk.getPos();
        int x1 = chunkPos.getMinBlockX();
        int x2 = chunkPos.getMaxBlockX();
        int z1 = chunkPos.getMinBlockZ();
        int z2 = chunkPos.getMaxBlockZ();
        int y1 = world.getMinBuildHeight();
        int y2 = world.getMaxBuildHeight();

        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = y1; y <= y2; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState blockState = world.getBlockState(pos);
                    Block block = blockState.getBlock();
                    if (block instanceof CraftingTableBlock) {
                        craftingTables.add(pos);
//                        showMessage(Component.literal("Found crafting table at " + pos));
                    }
                }
            }
        }
    }

    public static void onWorldRenderLast(WorldRenderContext context) {
        if (config.showDebuggingBorders) {
            if (chestMap.isEmpty()) return;

            PoseStack poseStack = context.matrixStack();
            if (poseStack == null) return;
            MultiBufferSource consumers = context.consumers();
            if (consumers == null) return;
            VertexConsumer vertexConsumer = consumers.getBuffer(RenderType.LINES);
            Vec3 cameraPos = context.camera().getPosition();

            for (var entry : chestMap.entrySet()) {
                BlockPos pos = entry.getKey();
                boolean isNull = entry.getValue() == null;

                float r = isNull ? 0.5f : 1.0f;
                float g = isNull ? 0.5f : 1.0f;
                float b = isNull ? 0.5f : 0.0f;
                float a = 1.0f;

                poseStack.pushPose();
                poseStack.translate(
                        pos.getX() - cameraPos.x,
                        pos.getY() - cameraPos.y,
                        pos.getZ() - cameraPos.z
                );
                LevelRenderer.renderLineBox(
                        poseStack, vertexConsumer,
                        0, 0, 0,
                        1, 1, 1,
                        r, g, b, a
                );
                poseStack.popPose();
            }
        }
    }

    public static void onDisconnect(ClientPacketListener clientPacketListener, Minecraft minecraft) {
        chestMap.clear();
        craftingTables.clear();
        showMessage(Component.literal("Disconnected from the server"));
    }

    private static final Set<Integer> prevKeyStates = new HashSet<>();

    private static void checkCustomKeybinds(Minecraft mc) {
        if (mc.player == null) return;
        long window = mc.getWindow().getWindow();

        Set<Integer> currentDown = new HashSet<>();

        for (KeybindEntry entry : keybindEntries) {
            checkKeybind(entry.keybind, window, currentDown, withScreenGuard(entry.action));
        }

        prevKeyStates.clear();
        prevKeyStates.addAll(currentDown);
    }

    private static void checkKeybind(CustomKeybind keybind, long window, Set<Integer> currentDown, Runnable action) {
        if (keybind.isUnbound()) return;
        int keyCode = keybind.getKeyCode();
        boolean isDown = GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
        if (isDown) {
            currentDown.add(keyCode);
            if (!prevKeyStates.contains(keyCode)) {
                int glfwMods = 0;
                if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                        GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) glfwMods |= GLFW.GLFW_MOD_CONTROL;
                if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
                        GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS) glfwMods |= GLFW.GLFW_MOD_ALT;
                if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                        GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) glfwMods |= GLFW.GLFW_MOD_SHIFT;
                if (keybind.matches(keyCode, glfwMods)) {
                    action.run();
                }
            }
        }
        if (!isDown) {
            prevKeyStates.remove(keyCode);
        }
    }

    public static void onClientTick(Minecraft mc) {
        checkCustomKeybinds(mc);
        switch (currentExplorationPhase) {
            case WAIT_NEXT -> {
                if (mc.screen == null) {
                    if (!scheduledBlockPosForExploration.isEmpty()
                    ) {
                        currentExplorationPhase = ExplorationPhase.WAIT_OPEN;
                        BlockPos nextPos = scheduledBlockPosForExploration.removeFirst();
                        lastInteractionRecord.updateRecord(nextPos, InteractionRecord.Type.CHEST);
                        interactBlock(nextPos);
                    } else {
                        currentExplorationPhase = ExplorationPhase.IDLE;
                        if (isCollectingItems) {
                            isCollectingItems = false;
                            boolean allFound = true;
                            for (var entry : neededItems.entrySet()) {
                                int remaining = entry.getValue();
                                if (remaining > 0) {
                                    allFound = false;
                                    showMessage(Component.literal("Warning: " + entry.getKey().getDescription().getString() + " x" + remaining + " not found"));
                                }
                            }
                            if (allFound) {
                                showMessage(Component.literal("All items collected successfully"));
                            } else {
                                showMessage(Component.literal("Item collection finished (some items missing)"));
                            }
                        } else {
                            showMessage(Component.literal("Finished exploring all nearby containers"));
                        }
                    }
                }
            }
            case WAIT_CLOSE -> {
                if (mc.screen != null) {
                    mc.screen.onClose();
                }
                currentExplorationPhase = ExplorationPhase.WAIT_NEXT;
            }
        }
    }

    public static InteractionResult onUseBlock(Player player, Level world, InteractionHand interactionHand, BlockHitResult hitResult) {
        if (currentExplorationPhase != ExplorationPhase.IDLE) return InteractionResult.PASS;
        BlockPos pos = hitResult.getBlockPos();
        if (world.getBlockState(pos).getBlock() instanceof ChestBlock) {
            lastInteractionRecord.updateRecord(pos, InteractionRecord.Type.CHEST);
        } else if (world.getBlockState(pos).getBlock() instanceof CraftingTableBlock) {
            lastInteractionRecord.updateRecord(pos, InteractionRecord.Type.CRAFTING_TABLE);
        } else {
            lastInteractionRecord.updateRecord(pos, InteractionRecord.Type.OTHER);
        }
        return InteractionResult.PASS;
    }

    public static void onChatMessage(String message) {
        if (mc.player != null) {
            if (message.contains("#opencraftingtable")) {
                openNearestCraftingTable((ClientLevel) mc.player.level());
            } else if (message.contains("#testcollectitems")) {
                startItemCollection();
            }
        }
    }

    public static void startItemCollection() {
        if (mc.player == null) return;

        Path csvPath = mc.gameDirectory.toPath().resolve("config/material_preparer/test.csv");
        if (!Files.exists(csvPath)) {
            showMessage(Component.literal("CSV file not found at " + csvPath));
            return;
        }

        Map<Item, Integer> items = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(",", 2);
                if (parts.length != 2) {
                    showMessage(Component.literal("Invalid CSV line: " + line));
                    continue;
                }
                String itemId = parts[0].trim();
                int count;
                try {
                    count = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    showMessage(Component.literal("Invalid count in CSV line: " + line));
                    continue;
                }

                ResourceLocation id = itemId.contains(":")
                        ? ResourceLocation.parse(itemId)
                        : ResourceLocation.parse("minecraft:" + itemId);
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item == Items.AIR) {
                    showMessage(Component.literal("Unknown item: " + itemId));
                    continue;
                }
                items.put(item, count);
            }
        } catch (IOException e) {
            showMessage(Component.literal("Failed to read CSV: " + e.getMessage()));
            return;
        }

        if (items.isEmpty()) {
            showMessage(Component.literal("No valid items found in CSV"));
            return;
        }

        neededItems.clear();
        neededItems.putAll(items);

        Vec3 playerPos = mc.player.getEyePosition();
        double range = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);

        scheduledBlockPosForExploration.clear();
        for (BlockPos pos : chestMap.keySet()) {
            if (playerPos.distanceTo(pos.getCenter()) <= range) {
                scheduledBlockPosForExploration.add(pos);
            }
        }
        scheduledBlockPosForExploration.sort(Comparator.comparingDouble(pos -> playerPos.distanceTo(pos.getCenter())));

        isCollectingItems = true;
        currentExplorationPhase = ExplorationPhase.WAIT_NEXT;
        showMessage(Component.literal("Collecting " + items.size() + " item types from " + scheduledBlockPosForExploration.size() + " chests..."));
    }

    public static void onScreenBeforeInit(Minecraft minecraft, Screen screen, int scaledWidth, int scaledHeight) {
        ScreenKeyboardEvents.allowKeyPress(screen).register(EventHandler::onScreenKeyboardKeyPress);

    }

    private static boolean onScreenKeyboardKeyPress(Screen screen, int key, int scancode, int modifiers) {
        if (capturingElement != null) {
            capturingElement.capture(key, scancode, modifiers);
            return key != GLFW.GLFW_KEY_ESCAPE;
        }
        return true;
    }
}
