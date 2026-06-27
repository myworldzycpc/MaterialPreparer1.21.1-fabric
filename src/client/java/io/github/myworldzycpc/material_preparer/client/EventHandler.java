package io.github.myworldzycpc.material_preparer.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.myworldzycpc.material_preparer.client.keybind.KeybindEntry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import static io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient.*;
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
        } else if (isChestLikeContainerBlockEntity(blockEntity)) {
            chestMap.put(pos, null);
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
        } else if (isChestLikeContainerBlockEntity(blockEntity)) {
            chestMap.remove(pos);
        }
    }

    public static void onChunkLoad(Level world, LevelChunk chunk) {
        // 记录区块内工作台
        ChunkPos chunkPos = chunk.getPos();
        int x1 = chunkPos.getMinBlockX();
        int x2 = chunkPos.getMaxBlockX();
        int z1 = chunkPos.getMinBlockZ();
        int z2 = chunkPos.getMaxBlockZ();
        int y1 = world.getMinY();
        int y2 = world.getMaxY();

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

    public static void onWorldRenderLast(LevelRenderContext context) {
        if (config.showDebuggingBorders) {
            PoseStack poseStack = context.poseStack();
            if (poseStack == null) return;
            MultiBufferSource consumers = context.consumers();
            if (consumers == null) return;
            VertexConsumer vertexConsumer = consumers.getBuffer(RenderTypes.LINES);
            Vec3 cameraPos = context.camera().getPosition();

            // 渲染普通容器
            for (var entry : chestMap.entrySet()) {
                BlockPos pos = entry.getKey();
                // 跳过黑名单和输出容器，它们会单独渲染
                if (blacklistedContainers.contains(pos)) continue;
                if (outputContainers.contains(pos)) continue;

                boolean isNull = entry.getValue() == null;

                float r = isNull ? 0.5f : 1.0f;
                float g = isNull ? 0.5f : 1.0f;
                float b = isNull ? 0.5f : 0.0f;
                float a = 1.0f;

                renderBox(poseStack, vertexConsumer, cameraPos, pos, r, g, b, a);
            }

            // 渲染黑名单容器（黑色）
            for (BlockPos pos : blacklistedContainers) {
                renderBox(poseStack, vertexConsumer, cameraPos, pos, 0.0f, 0.0f, 0.0f, 1.0f);
            }

            // 渲染输出容器（红色）
            for (BlockPos pos : outputContainers) {
                renderBox(poseStack, vertexConsumer, cameraPos, pos, 1.0f, 0.0f, 0.0f, 1.0f);
            }
        }
    }

    private static void renderBox(PoseStack poseStack, VertexConsumer vertexConsumer, Vec3 cameraPos, BlockPos pos, float r, float g, float b, float a) {
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

    public static void onDisconnect(ClientPacketListener clientPacketListener, Minecraft minecraft) {
        chestMap.clear();
        craftingTables.clear();
        blacklistedContainers.clear();
        outputContainers.clear();
        scheduledBlockPosForExploration.clear();
        scheduledOutputContainers.clear();
        suspendedContainerPos = null;
        showMessage(Component.translatable("message.material_preparer.disconnected"));
    }

    private static final Set<Integer> prevKeyStates = new HashSet<>();

    private static void checkCustomKeybinds(Minecraft mc) {
        if (mc.player == null) return;
        long window = mc.getWindow().handle();

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
        if (mc.player == null) return;
        tickCounter++;
        processContainerClickQueue();
        checkCustomKeybinds(mc);
        switch (currentExplorationPhase) {
            case WAIT_NEXT -> {
                if (mc.gui.screen() == null) {
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
                                    showMessage(Component.translatable("message.material_preparer.warning_item_not_found", entry.getKey().getDefaultInstance().getDisplayName(), remaining));
                                }
                            }
                            if (allFound) {
                                showMessage(Component.translatable("message.material_preparer.all_items_collected"));
                            } else {
                                showMessage(Component.translatable("message.material_preparer.item_collection_finished"));
                            }
                        } else {
                            showMessage(Component.translatable("message.material_preparer.finished_exploring"));
                        }
                    }
                }
            }
            case PROCESS_NEXT_ITEM -> {
                List<ItemStack> chestItems = null;
                if (mc.player.containerMenu instanceof ChestMenu chestMenu) {
                    chestItems = ((SimpleContainer) chestMenu.getContainer()).getItems();
                } else if (mc.player.containerMenu instanceof ShulkerBoxMenu shulkerBoxMenu) {
                    chestItems = shulkerBoxMenu.getItems().subList(0, 27);
                }
                if (chestItems != null) {
                    boolean found = false;
                    for (int i = 0; i < chestItems.size(); i++) {
                        ItemStack stack = chestItems.get(i);
                        if (!stack.isEmpty() && neededItems.containsKey(stack.getItem())) {
                            int remaining = neededItems.get(stack.getItem());
                            if (remaining > 0) {
                                int moving = Math.min(remaining, stack.getCount());
                                neededItems.put(stack.getItem(), remaining - moving);
                                if (config.alwaysQuickMove) {
                                    quickMoveContainerSlot(i);
                                } else {
                                    preciseMoveContainerSlot(i, moving);
                                }
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) {
                        currentExplorationPhase = ExplorationPhase.WAIT_ITEM_MOVE;
                    } else {
                        // 所有物品都处理完了
                        currentExplorationPhase = ExplorationPhase.WAIT_CLOSE;
                    }
                } else {
                    // 不是箱子菜单，直接关闭
                    currentExplorationPhase = ExplorationPhase.WAIT_CLOSE;
                }
            }
            case WAIT_ITEM_MOVE -> {
                if (!containerClickQueue.isEmpty()) return;
                // 点击队列清空了，检查背包是否满了
                if (isPlayerInventoryFull()) {
                    // 背包满了，挂起当前容器，去输出容器转移
                    suspendedContainerPos = lastInteractionRecord.pos;
                    currentExplorationPhase = ExplorationPhase.WAIT_CLOSE;
                } else {
                    // 背包没满，继续处理下一个物品
                    currentExplorationPhase = ExplorationPhase.PROCESS_NEXT_ITEM;
                }
            }
            case WAIT_CLOSE -> {
                if (!containerClickQueue.isEmpty()) return;
                if (mc.gui.screen() != null) {
                    mc.gui.screen().onClose();
                }
                // 如果是收集模式，检查背包是否已满，需要转移到输出容器
                if (isCollectingItems && isPlayerInventoryFull()) {
                    // 初始化计划输出容器列表
                    scheduledOutputContainers.clear();
                    List<BlockPos> notFullContainers = new ArrayList<>();
                    List<BlockPos> otherContainers = new ArrayList<>();
                    Vec3 playerPos = mc.player.getEyePosition();
                    double range = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);

                    for (BlockPos pos : outputContainers) {
                        if (playerPos.distanceTo(Vec3.atCenterOf(pos)) <= range) {
                            if (isContainerNotFull(pos)) {
                                notFullContainers.add(pos);
                            } else {
                                otherContainers.add(pos);
                            }
                        }
                    }
                    notFullContainers.sort(Comparator.comparingDouble(pos -> playerPos.distanceTo(Vec3.atCenterOf(pos))));
                    otherContainers.sort(Comparator.comparingDouble(pos -> playerPos.distanceTo(Vec3.atCenterOf(pos))));
                    scheduledOutputContainers.addAll(notFullContainers);
                    scheduledOutputContainers.addAll(otherContainers);
                    tryNextOutputContainer();
                } else {
                    currentExplorationPhase = ExplorationPhase.WAIT_NEXT;
                }
            }
            case WAIT_OUTPUT_OPEN -> {
                // 等待输出容器打开，由 MixinClientPacketListener 处理
            }
            case WAIT_OUTPUT_SET_CONTENTS -> {
                // 等待输出容器内容，由 MixinClientPacketListener 处理
            }
            case TRANSFERRING_TO_OUTPUT -> {
                // 正在转移物品，等待点击队列清空
                if (containerClickQueue.isEmpty()) {
                    currentExplorationPhase = ExplorationPhase.WAIT_OUTPUT_CLOSE;
                }
            }
            case WAIT_OUTPUT_CLOSE -> {
                if (!containerClickQueue.isEmpty()) return;
                if (mc.gui.screen() != null) {
                    mc.gui.screen().onClose();
                }
                // 检查转移后背包是否还是满的
                if (isPlayerInventoryFull()) {
                    // 背包还是满的，尝试下一个输出容器
                    showDebugMessage("Inventory is still full, try the next output container");
                    tryNextOutputContainer();
                } else {
                    // 还有空间
                    scheduledOutputContainers.clear();
                    if (suspendedContainerPos != null) {
                        // 有挂起的容器，回去继续处理
                        currentExplorationPhase = ExplorationPhase.WAIT_OPEN;
                        lastInteractionRecord.updateRecord(suspendedContainerPos, InteractionRecord.Type.CHEST);
                        interactBlock(suspendedContainerPos);
                        suspendedContainerPos = null;
                    } else {
                        // 没有挂起的容器，继续收集
                        currentExplorationPhase = ExplorationPhase.WAIT_NEXT;
                    }
                }
            }
        }
    }

    public static InteractionResult onUseBlock(Player player, Level world, InteractionHand interactionHand, BlockHitResult hitResult) {
        if (currentExplorationPhase != ExplorationPhase.IDLE) return InteractionResult.PASS;
        BlockPos pos = hitResult.getBlockPos();
        if (isChestLikeContainerBlock(world.getBlockState(pos).getBlock())) {
            lastInteractionRecord.updateRecord(pos, InteractionRecord.Type.CHEST);
        } else if (world.getBlockState(pos).getBlock() instanceof CraftingTableBlock) {
            lastInteractionRecord.updateRecord(pos, InteractionRecord.Type.CRAFTING_TABLE);
        } else {
            lastInteractionRecord.updateRecord(pos, InteractionRecord.Type.OTHER);
        }
        return InteractionResult.PASS;
    }

    // 尝试下一个输出容器
    private static void tryNextOutputContainer() {
        if (scheduledOutputContainers.isEmpty()) {
            // 所有输出容器都尝试过了，停止收集
            currentExplorationPhase = ExplorationPhase.IDLE;
            isCollectingItems = false;
            showMessage(Component.translatable("message.material_preparer.inventory_and_output_full"));
            return;
        }

        BlockPos outputPos = scheduledOutputContainers.removeFirst();
        currentExplorationPhase = ExplorationPhase.WAIT_OUTPUT_OPEN;
        lastInteractionRecord.updateRecord(outputPos, InteractionRecord.Type.CHEST);
        if (mc.level != null && isChestLikeContainerBlock(mc.level.getBlockState(outputPos).getBlock())) {
            interactBlock(outputPos);
        } else {
            // 不是箱子，直接跳到 WAIT_OUTPUT_CLOSE，继续尝试下一个
            currentExplorationPhase = ExplorationPhase.WAIT_OUTPUT_CLOSE;
        }
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

        Path csvPath = mc.gameDirectory.toPath().resolve("config/material_preparer/item_list.csv");
        if (!Files.exists(csvPath)) {
            showMessage(Component.translatable("message.material_preparer.csv_file_not_found", csvPath.toString()));
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
                    showMessage(Component.translatable("message.material_preparer.invalid_csv_line", line));
                    continue;
                }
                String itemId = parts[0].trim();
                int count;
                try {
                    count = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    showMessage(Component.translatable("message.material_preparer.invalid_csv_count", line));
                    continue;
                }

                Identifier id = itemId.contains(":")
                        ? Identifier.parse(itemId)
                        : Identifier.parse("minecraft:" + itemId);
                Optional<Holder.Reference<Item>> itemReference = BuiltInRegistries.ITEM.get(id);
                if (itemReference.isPresent()) {
                    Item item = itemReference.get().value();
                    if (item == Items.AIR) {
                        showMessage(Component.translatable("message.material_preparer.unknown_item", itemId));
                        continue;
                    }
                    items.put(item, count);
                }
            }
        } catch (IOException e) {
            showMessage(Component.translatable("message.material_preparer.failed_read_csv", e.getMessage()));
            return;
        }

        if (items.isEmpty()) {
            showMessage(Component.translatable("message.material_preparer.no_valid_items_in_csv"));
            return;
        }

        neededItems.clear();
        neededItems.putAll(items);

        // 如果开启了忽略已有物品，从需求中减去玩家背包和输出容器中已有的数量
        if (config.ignoreExistingItems) {
            // 减去玩家背包中的物品
            Inventory playerInv = mc.player.getInventory();
            for (int i = 0; i < 36; i++) { // 主背包 + 快捷栏共 36 格
                ItemStack stack = playerInv.getItem(i);
                if (!stack.isEmpty()) {
                    Item item = stack.getItem();
                    if (neededItems.containsKey(item)) {
                        int remaining = neededItems.get(item) - stack.getCount();
                        if (remaining <= 0) {
                            neededItems.remove(item);
                        } else {
                            neededItems.put(item, remaining);
                        }
                    }
                }
            }

            // 减去输出容器中已缓存的物品
            for (BlockPos pos : outputContainers) {
                List<ItemStack> containerItems = chestMap.get(pos);
                if (containerItems == null) continue;
                for (ItemStack stack : containerItems) {
                    if (stack.isEmpty()) continue;
                    Item item = stack.getItem();
                    if (neededItems.containsKey(item)) {
                        int remaining = neededItems.get(item) - stack.getCount();
                        if (remaining <= 0) {
                            neededItems.remove(item);
                        } else {
                            neededItems.put(item, remaining);
                        }
                    }
                }
            }
        }

        // 如果没有需要收集的物品，直接返回
        if (neededItems.isEmpty()) {
            showMessage(Component.translatable("message.material_preparer.all_items_already_in_inventory"));
            return;
        }

        Vec3 playerPos = mc.player.getEyePosition();
        double range = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);

        scheduledBlockPosForExploration.clear();
        suspendedContainerPos = null;
        for (BlockPos pos : chestMap.keySet()) {
            if (playerPos.distanceTo(Vec3.atCenterOf(pos)) <= range) {
                if (blacklistedContainers.contains(pos)) continue;
                if (outputContainers.contains(pos)) continue;
                scheduledBlockPosForExploration.add(pos);
            }
        }
        scheduledBlockPosForExploration.sort(Comparator.comparingDouble(pos -> playerPos.distanceTo(Vec3.atCenterOf(pos))));

        isCollectingItems = true;
        currentExplorationPhase = ExplorationPhase.WAIT_NEXT;
        showMessage(Component.translatable("message.material_preparer.collecting_items", items.size(), scheduledBlockPosForExploration.size()));
    }

    public static void onScreenBeforeInit(Minecraft minecraft, Screen screen, int scaledWidth, int scaledHeight) {
        ScreenKeyboardEvents.allowKeyPress(screen).register(EventHandler::onScreenKeyboardKeyPress);

    }

    private static boolean onScreenKeyboardKeyPress(Screen screen, KeyEvent keyEvent) {
        int key = keyEvent.key();
        int scancode = keyEvent.scancode();
        int modifiers = keyEvent.modifiers();
        if (capturingElement != null) {
            capturingElement.capture(key, scancode, modifiers);
            return key != GLFW.GLFW_KEY_ESCAPE;
        }
        return true;
    }
}
