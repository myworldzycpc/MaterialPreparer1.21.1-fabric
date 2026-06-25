package io.github.myworldzycpc.material_preparer.client;

import io.github.myworldzycpc.material_preparer.client.config.ItemEntry;
import io.github.myworldzycpc.material_preparer.client.config.ItemListSerializer;
import io.github.myworldzycpc.material_preparer.client.config.MaterialPreparerConfig;
import io.github.myworldzycpc.material_preparer.client.screen.LitematicaFileSelectorScreen;
import io.github.myworldzycpc.material_preparer.client.keybind.KeybindingElement;
import io.github.myworldzycpc.material_preparer.client.keybind.KeybindEntry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.*;
import java.util.List;

import io.github.myworldzycpc.material_preparer.client.keybind.CustomKeybind;

import static io.github.myworldzycpc.material_preparer.client.ModMenuIntegration.getModConfigScreen;

public class MaterialPreparerClient implements ClientModInitializer {
    public static Map<BlockPos, List<ItemStack>> chestMap = new HashMap<>();
    public static Set<BlockPos> craftingTables = new HashSet<>();
    public static Set<BlockPos> blacklistedContainers = new HashSet<>();
    public static Set<BlockPos> outputContainers = new HashSet<>();
    public static final InteractionRecord lastInteractionRecord = new InteractionRecord();
    public static ExplorationPhase currentExplorationPhase = ExplorationPhase.IDLE;
    public static List<BlockPos> scheduledBlockPosForExploration = new ArrayList<>();
    public static List<BlockPos> scheduledOutputContainers = new ArrayList<>();
    public static BlockPos suspendedContainerPos = null;
    public static boolean isCollectingItems = false;
    public static Map<Item, Integer> neededItems = new HashMap<>();
    public static List<ItemEntry> itemList = new ArrayList<>();

    // Container click rate limiting
    public static final Queue<ContainerClickAction> containerClickQueue = new LinkedList<>();
    public static int lastContainerClickTick = -1;
    public static int tickCounter = 0;

    public record ContainerClickAction(int slot, ClickType clickType, int button) {
    }

    public static Minecraft mc = Minecraft.getInstance();

    public static MaterialPreparerConfig config;


    public static KeybindEntry craftingTableKeybind = new KeybindEntry(
            () -> openNearestCraftingTable(mc.level),
            val -> config.craftingTableKeybind = val,
            () -> config.craftingTableKeybind
    );

    public static KeybindEntry bordersKeybind = new KeybindEntry(
            () -> {
                config.showDebuggingBorders = !config.showDebuggingBorders;
                showMessage(Component.translatable(config.showDebuggingBorders ? "message.material_preparer.debug_borders_on" : "message.material_preparer.debug_borders_off"));
            },
            val -> config.showDebuggingBordersKeybind = val,
            () -> config.showDebuggingBordersKeybind
    );

    public static KeybindEntry exploreAllNearbyContainerKeybind = new KeybindEntry(
            MaterialPreparerClient::exploreAllNearbyContainers,
            val -> config.exploreAllNearbyContainerKeybind = val,
            () -> config.exploreAllNearbyContainerKeybind
    );

    public static KeybindEntry openScreenKeybind = new KeybindEntry(
            () -> {
                if (mc.screen == null) mc.setScreen(getModConfigScreen(null));
            },
            val -> config.openScreenKeybind = val,
            () -> config.openScreenKeybind
    );

    public static KeybindEntry collectItemsKeybind = new KeybindEntry(
            EventHandler::startItemCollection,
            val -> config.collectItemsKeybind = val,
            () -> config.collectItemsKeybind
    );

    public static KeybindEntry loadItemListFromLitematicaKeybind = new KeybindEntry(
            MaterialPreparerClient::loadItemListFromLitematica,
            val -> config.loadItemListFromLitematicaKeybind = val,
            () -> config.loadItemListFromLitematicaKeybind
    );

    private static void loadItemListFromLitematica() {
        // 打开文件选择屏幕
        LitematicaFileSelectorScreen screen = new LitematicaFileSelectorScreen(mc.screen);
        mc.setScreen(screen);
    }

    public static KeybindEntry loadItemListFromCSVKeybind = new KeybindEntry(
            MaterialPreparerClient::loadItemListFromCSV,
            val -> config.loadItemListFromCSVKeybind = val,
            () -> config.loadItemListFromCSVKeybind
    );

    private static void loadItemListFromCSV() {
        showMessage(Component.translatable("message.material_preparer.not_implemented"));
    }

    public static KeybindEntry alwaysQuickMoveKeybind = new KeybindEntry(
            () -> {
                config.alwaysQuickMove = !config.alwaysQuickMove;
                showMessage(Component.translatable(config.alwaysQuickMove ? "message.material_preparer.always_quick_move_on" : "message.material_preparer.always_quick_move_off"));
            },
            val -> config.alwaysQuickMoveKeybind = val,
            () -> config.alwaysQuickMoveKeybind
    );

    public static KeybindEntry ignoreExistingItemsKeybind = new KeybindEntry(
            () -> {
                config.ignoreExistingItems = !config.ignoreExistingItems;
                showMessage(Component.translatable(config.ignoreExistingItems ? "message.material_preparer.ignore_existing_items_on" : "message.material_preparer.ignore_existing_items_off"));
            },
            val -> config.ignoreExistingItemsKeybind = val,
            () -> config.ignoreExistingItemsKeybind
    );

    public static KeybindEntry toggleBlacklistKeybind = new KeybindEntry(
            MaterialPreparerClient::toggleBlacklist,
            val -> config.toggleBlacklistKeybind = val,
            () -> config.toggleBlacklistKeybind
    );

    public static KeybindEntry toggleOutputContainerKeybind = new KeybindEntry(
            MaterialPreparerClient::toggleOutputContainer,
            val -> config.toggleOutputContainerKeybind = val,
            () -> config.toggleOutputContainerKeybind
    );

    public static KeybindEntry clearAllMarkersKeybind = new KeybindEntry(
            MaterialPreparerClient::clearAllMarkers,
            val -> config.clearAllMarkersKeybind = val,
            () -> config.clearAllMarkersKeybind
    );

    public static KeybindEntry showDebugMessagesKeybind = new KeybindEntry(
            () -> {
                config.showDebugMessages = !config.showDebugMessages;
                showMessage(Component.translatable(config.ignoreExistingItems ? "message.material_preparer.show_debug_messages_on" : "message.material_preparer.show_debug_messages_off"));
            },
            val -> config.showDebugMessagesKeybind = val,
            () -> config.showDebugMessagesKeybind
    );

    public static List<KeybindEntry> keybindEntries = Arrays.asList(
            craftingTableKeybind,
            bordersKeybind,
            exploreAllNearbyContainerKeybind,
            openScreenKeybind,
            collectItemsKeybind,
            loadItemListFromLitematicaKeybind,
            loadItemListFromCSVKeybind,
            alwaysQuickMoveKeybind,
            ignoreExistingItemsKeybind,
            toggleBlacklistKeybind,
            toggleOutputContainerKeybind,
            clearAllMarkersKeybind,
            showDebugMessagesKeybind
    );

    public static KeybindingElement capturingElement = null;

    public static void showMessage(Component component) {
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.translatable(
                    "%s %s",
                    Component.translatable(
                            "[%s]",
                            Component.literal("Material Preparer").withStyle(ChatFormatting.GOLD)
                    ).withStyle(ChatFormatting.DARK_AQUA),
                    component)
            );
        }
    }

    public static void showDebugMessage(String message) {
        if (!config.showDebugMessages) return;
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.translatable(
                    "%s %s",
                    Component.translatable(
                            "[%s]",
                            Component.literal("Material Preparer DEBUG").withStyle(ChatFormatting.GRAY)
                    ).withStyle(ChatFormatting.DARK_AQUA),
                    message)
            );
        } else {
            System.out.println(message);
        }
    }

    public static void toggleBlacklist() {
        if (mc.player == null || mc.level == null) return;
        if (!(mc.hitResult instanceof BlockHitResult blockHitResult)) {
            showMessage(Component.translatable("message.material_preparer.no_block_targeted"));
            return;
        }
        BlockPos pos = blockHitResult.getBlockPos();
        if (blacklistedContainers.contains(pos)) {
            blacklistedContainers.remove(pos);
            showMessage(Component.translatable("message.material_preparer.removed_from_blacklist", pos.toShortString()));
        } else {
            blacklistedContainers.add(pos);
            showMessage(Component.translatable("message.material_preparer.added_to_blacklist", pos.toShortString()));
        }
    }

    public static void toggleOutputContainer() {
        if (mc.player == null || mc.level == null) return;
        if (!(mc.hitResult instanceof BlockHitResult blockHitResult)) {
            showMessage(Component.translatable("message.material_preparer.no_block_targeted"));
            return;
        }
        BlockPos pos = blockHitResult.getBlockPos();
        if (outputContainers.contains(pos)) {
            outputContainers.remove(pos);
            showMessage(Component.translatable("message.material_preparer.removed_output_container", pos.toShortString()));
        } else {
            outputContainers.add(pos);
            showMessage(Component.translatable("message.material_preparer.set_as_output_container", pos.toShortString()));
        }
    }

    public static void clearAllMarkers() {
        blacklistedContainers.clear();
        outputContainers.clear();
        showMessage(Component.translatable("message.material_preparer.all_markers_cleared"));
    }

    // 检查玩家背包是否已满（主背包 + 快捷栏）
    public static boolean isPlayerInventoryFull() {
        if (mc.player == null) return true;
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < 36; i++) {
            if (inv.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // 找到最近的输出容器
    public static BlockPos findNearestOutputContainer() {
        if (mc.player == null) return null;
        if (outputContainers.isEmpty()) return null;

        Vec3 playerPos = mc.player.getEyePosition();
        double range = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        BlockPos nearest = null;
        double minDist = Double.MAX_VALUE;

        for (BlockPos pos : outputContainers) {
            double dist = playerPos.distanceTo(pos.getCenter());
            if (dist <= range && dist < minDist) {
                minDist = dist;
                nearest = pos;
            }
        }

        return nearest;
    }

    // 判断容器是否没满（基于 chestMap 缓存）
    public static boolean isContainerNotFull(BlockPos pos) {
        List<ItemStack> items = chestMap.get(pos);
        if (items == null) return true; // 未知的容器，假设没满
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                return true; // 有空槽位，说明没满
            }
        }
        return false; // 所有槽位都有物品，满了
    }

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.

        MaterialPreparerConfig.HANDLER.load();
        config = MaterialPreparerConfig.HANDLER.instance();

        // Load item list from CSV
        Path csvPath = FabricLoader.getInstance().getConfigDir().resolve("material_preparer/item_list.csv");
        itemList = ItemListSerializer.loadFromCsv(csvPath);

        for (KeybindEntry keybindEntry : keybindEntries) {
            CustomKeybind.deserialize(keybindEntry.configLoader.get()).copyTo(keybindEntry.keybind);
        }

        ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register(EventHandler::onBlockEntityLoad);
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(EventHandler::onBlockEntityUnload);
        ClientChunkEvents.CHUNK_LOAD.register(EventHandler::onChunkLoad);
        ClientSendMessageEvents.CHAT.register(EventHandler::onChatMessage);
        WorldRenderEvents.LAST.register(EventHandler::onWorldRenderLast);
        ClientPlayConnectionEvents.DISCONNECT.register(EventHandler::onDisconnect);
        ClientTickEvents.END_CLIENT_TICK.register(EventHandler::onClientTick);
        UseBlockCallback.EVENT.register(EventHandler::onUseBlock);
        ScreenEvents.BEFORE_INIT.register(EventHandler::onScreenBeforeInit);

    }

    public static boolean openNearestCraftingTable(ClientLevel world) {
        if (mc.player == null) {
            return false;
        }

        // 找到最近的工作台，如果没有找到则返回false
        if (craftingTables.isEmpty()) {
            showMessage(Component.translatable("message.material_preparer.no_crafting_tables_found"));
            return false;
        }

        Vec3 playerLocation = mc.player.getEyePosition();
        double maxDistance = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        BlockPos closestCraftingTable = null;
        for (BlockPos craftingTable : craftingTables) {
            double distance = playerLocation.distanceTo(craftingTable.getCenter());
            if (distance < maxDistance) {
                maxDistance = distance;
                closestCraftingTable = craftingTable;
            }
        }
        if (closestCraftingTable == null) {
            showMessage(Component.translatable("message.material_preparer.no_crafting_tables_in_range"));
            return false;
        }

        interactBlock(closestCraftingTable);
        showMessage(Component.translatable("message.material_preparer.opened_crafting_table", closestCraftingTable.toShortString()));
        return true;
    }

    static void interactBlock(BlockPos pos) {
        if (mc.player == null) return;
        mc.player.connection.send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, new BlockHitResult(
                pos.getCenter(),
                Direction.UP,
                pos,
                false
        ), 1));
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    public static void quickMoveContainerSlot(int slot) {
        clickContainerSlot(slot, ClickType.QUICK_MOVE, 0);
    }

    public static void clickContainerSlot(int slot, ClickType clickType, int k) {
        if (config == null || config.minClickInterval <= 0) {
            executeContainerClick(slot, clickType, k);
        } else {
            containerClickQueue.add(new ContainerClickAction(slot, clickType, k));
        }
    }

    private static void executeContainerClick(int slot, ClickType clickType, int k) {
        if (mc.player == null) return;
        if (mc.gameMode == null) return;
        int containerId = mc.player.containerMenu.containerId;
        mc.gameMode.handleInventoryMouseClick(containerId, slot, k, clickType, mc.player);
    }

    public static void processContainerClickQueue() {
        if (config == null || config.minClickInterval <= 0) return;
        if (containerClickQueue.isEmpty()) return;

        if (lastContainerClickTick < 0 || (tickCounter - lastContainerClickTick) >= config.minClickInterval) {
            ContainerClickAction action = containerClickQueue.poll();
            if (action != null) {
                executeContainerClick(action.slot(), action.clickType(), action.button());
                lastContainerClickTick = tickCounter;
            }
        }
    }

    /**
     * 精确移动指定数量的物品从容器槽位到玩家背包
     * 不使用快速移动，而是通过拿起、放回多余、放置的步骤精确控制数量
     */
    public static void preciseMoveContainerSlot(int sourceSlot, int count) {
        if (mc.player == null) return;

        // 获取源槽位的物品堆叠
        ItemStack sourceStack = mc.player.containerMenu.getSlot(sourceSlot).getItem();
        if (sourceStack.isEmpty()) return;

        Item item = sourceStack.getItem();
        int stackSize = sourceStack.getCount();

        // 如果需要的数量 >= 堆叠大小，直接快速移动整个堆叠
        if (count >= stackSize) {
            quickMoveContainerSlot(sourceSlot);
            return;
        }

        int excess = stackSize - count;

        // 步骤1: 左键拿起整个堆叠
        clickContainerSlot(sourceSlot, ClickType.PICKUP, 0);

        // 步骤2: 右键放回多余的物品（每次放回1个，共 excess 次）
        for (int i = 0; i < excess; i++) {
            clickContainerSlot(sourceSlot, ClickType.PICKUP, 1);
        }

        // 步骤3: 将鼠标上的物品放到玩家背包
        placeCursorItemIntoPlayerInventory(item, count);
    }

    /**
     * 将鼠标上的物品放入玩家背包
     * 优先放到相同物品的未满堆叠，然后放到空格子
     */
    private static void placeCursorItemIntoPlayerInventory(Item item, int remainingCount) {
        if (mc.player == null) return;
        if (mc.player.containerMenu == null) return;

        int playerInvStart = getPlayerInventoryStartSlot();
        int playerInvEnd = playerInvStart + 36; // 36 slots total (27 main + 9 hotbar)

        int remaining = remainingCount;

        // 第一遍：找到相同物品且未满的槽位，优先放背包主体，再放快捷栏
        for (int i = playerInvStart; i < playerInvEnd; i++) {
            if (remaining <= 0) break;

            ItemStack stack = mc.player.containerMenu.getSlot(i).getItem();
            if (!stack.isEmpty() && stack.is(item)) {
                int maxStackSize = stack.getMaxStackSize();
                int space = maxStackSize - stack.getCount();
                if (space > 0) {
                    // 左键点击放下物品（会尽可能多放）
                    clickContainerSlot(i, ClickType.PICKUP, 0);
                    // 估算剩余数量（实际可能因为堆叠限制而不同）
                    remaining = Math.max(0, remaining - space);
                }
            }
        }

        // 第二遍：如果还有剩余，找空格子
        if (remaining > 0) {
            for (int i = playerInvStart; i < playerInvEnd; i++) {
                if (remaining <= 0) break;

                ItemStack stack = mc.player.containerMenu.getSlot(i).getItem();
                if (stack.isEmpty()) {
                    clickContainerSlot(i, ClickType.PICKUP, 0);
                    remaining = 0; // 左键放下所有物品（假设空格子能放下）
                }
            }
        }

        // 如果还有剩余（背包满了），物品会留在鼠标上，这里不做特殊处理
    }

    /**
     * 获取玩家背包在容器菜单中的起始槽位
     */
    public static int getPlayerInventoryStartSlot() {
        if (mc.player == null) return 0;
        // 玩家背包总是在最后 36 个槽位
        return mc.player.containerMenu.slots.size() - 36;
    }


    public static BlockPos getOppositeChestPos(BlockPos pos, Direction direction, ChestType chestType) {
        return switch (chestType) {
            case SINGLE -> pos;
            case LEFT -> switch (direction) {
                case NORTH -> pos.east();
                case SOUTH -> pos.west();
                case EAST -> pos.south();
                case WEST -> pos.north();
                default -> throw new IllegalArgumentException("Invalid direction: " + direction);
            };
            case RIGHT -> switch (direction) {
                case NORTH -> pos.west();
                case SOUTH -> pos.east();
                case EAST -> pos.north();
                case WEST -> pos.south();
                default -> throw new IllegalArgumentException("Invalid direction: " + direction);
            };
        };
    }

    public static void updateChestItems(BlockPos pos, List<ItemStack> items) {
        chestMap.put(pos, items);
    }


    public static void exploreAllNearbyContainers() {
        if (mc.player == null) return;
        ClientLevel world = mc.level;
        if (world == null) return;

        Vec3 playerPos = mc.player.getEyePosition();
        double range = mc.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);

        List<BlockPos> inRange = new ArrayList<>();
        for (BlockPos pos : chestMap.keySet()) {
            if (playerPos.distanceTo(pos.getCenter()) <= range) {
                if (blacklistedContainers.contains(pos)) continue;
                inRange.add(pos);
            }
        }

        if (inRange.isEmpty()) {
            showMessage(Component.translatable("message.material_preparer.no_nearby_containers"));
            return;
        }

        inRange.sort(Comparator.comparingDouble(pos -> playerPos.distanceTo(pos.getCenter())));

        scheduledBlockPosForExploration.clear();
        scheduledBlockPosForExploration.addAll(inRange);
        currentExplorationPhase = ExplorationPhase.WAIT_NEXT;
        showMessage(Component.translatable("message.material_preparer.exploring_containers", inRange.size()));
    }

    public static boolean isChestLikeContainerBlock(Block block) {
        return block instanceof ChestBlock ||
                block instanceof ShulkerBoxBlock ||
                block instanceof BarrelBlock;
    }

    public static boolean isChestLikeContainerBlockEntity(BlockEntity blockEntity) {
        return blockEntity instanceof ChestBlockEntity ||
                blockEntity instanceof ShulkerBoxBlockEntity ||
                blockEntity instanceof BarrelBlockEntity;
    }
}