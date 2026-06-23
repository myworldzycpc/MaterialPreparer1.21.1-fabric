package io.github.myworldzycpc.material_preparer.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.myworldzycpc.material_preparer.client.config.ItemEntry;
import io.github.myworldzycpc.material_preparer.client.config.MaterialPreparerConfig;
import io.github.myworldzycpc.material_preparer.client.keybind.IKeybindingElement;
import io.github.myworldzycpc.material_preparer.client.keybind.KeybindEntry;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.List;
import java.util.function.Supplier;

import io.github.myworldzycpc.material_preparer.client.keybind.CustomKeybind;

import static io.github.myworldzycpc.material_preparer.client.ModMenuIntegration.getModConfigScreen;

public class MaterialPreparerClient implements ClientModInitializer {
    public static Map<BlockPos, List<ItemStack>> chestMap = new HashMap<>();
    public static Set<BlockPos> craftingTables = new HashSet<>();
    public static final InteractionRecord lastInteractionRecord = new InteractionRecord();
    public static ExplorationPhase currentExplorationPhase = ExplorationPhase.IDLE;
    public static List<BlockPos> scheduledBlockPosForExploration = new ArrayList<>();
    public static boolean isCollectingItems = false;
    public static Map<Item, Integer> neededItems = new HashMap<>();
    public static List<ItemEntry> itemList = new ArrayList<>();

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
                showMessage(Component.literal("Debugging borders: " + (config.showDebuggingBorders ? "ON" : "OFF")));
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
        showMessage(Component.literal("Not Implemented!"));
    }

    public static KeybindEntry loadItemListFromCSVKeybind = new KeybindEntry(
            MaterialPreparerClient::loadItemListFromCSV,
            val -> config.loadItemListFromCSVKeybind = val,
            () -> config.loadItemListFromCSVKeybind
    );

    private static void loadItemListFromCSV() {
        showMessage(Component.literal("Not Implemented!"));
    }

    public static List<KeybindEntry> keybindEntries = Arrays.asList(
            craftingTableKeybind,
            bordersKeybind,
            exploreAllNearbyContainerKeybind,
            openScreenKeybind,
            collectItemsKeybind,
            loadItemListFromLitematicaKeybind,
            loadItemListFromCSVKeybind
    );

    public static IKeybindingElement capturingElement = null;

    public static void showMessage(Component component) {
        if (mc.player != null) {
            mc.player.sendSystemMessage(component);
        }
    }

    @Override
    public void onInitializeClient() {
        // This entrypoint is suitable for setting up client-specific logic, such as rendering.

        MaterialPreparerConfig.HANDLER.load();
        config = MaterialPreparerConfig.HANDLER.instance();

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
            showMessage(Component.literal("No crafting tables found"));
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
            showMessage(Component.literal("No crafting tables in range"));
            return false;
        }

        interactBlock(closestCraftingTable);
        showMessage(Component.literal("Opened nearest crafting table at " + closestCraftingTable));
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

    public static void clickContainerSlot(int slot) {
        if (mc.player == null) {
            return;
        }
        int containerId = mc.player.containerMenu.containerId;
        AbstractContainerMenu containerMenu = MaterialPreparerClient.mc.player.containerMenu;
        int stateId = containerMenu.getStateId();
        ItemStack itemStack = containerMenu.getSlot(0).getItem();
        Int2ObjectArrayMap<ItemStack> slots = new Int2ObjectArrayMap<>();
        slots.put(0, ItemStack.EMPTY);
        slots.put(-1, itemStack);
        ServerboundContainerClickPacket packet = new ServerboundContainerClickPacket(containerId, stateId, 0, 0, ClickType.PICKUP, itemStack, slots);
        MaterialPreparerClient.mc.player.connection.send(packet);
    }

    public static void quickMoveContainerSlot(int slot) {
        if (mc.player == null) return;
        if (mc.gameMode == null) return;
        int containerId = mc.player.containerMenu.containerId;
        mc.gameMode.handleInventoryMouseClick(containerId, slot, 0, ClickType.QUICK_MOVE, mc.player);
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
                inRange.add(pos);
            }
        }

        if (inRange.isEmpty()) {
            showMessage(Component.literal("No nearby containers to explore"));
            return;
        }

        inRange.sort(Comparator.comparingDouble(pos -> playerPos.distanceTo(pos.getCenter())));

        scheduledBlockPosForExploration.clear();
        scheduledBlockPosForExploration.addAll(inRange);
        currentExplorationPhase = ExplorationPhase.WAIT_NEXT;
        showMessage(Component.literal("Exploring " + inRange.size() + " nearby containers..."));
    }

}