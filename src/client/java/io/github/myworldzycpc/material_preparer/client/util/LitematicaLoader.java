package io.github.myworldzycpc.material_preparer.client.util;

import io.github.myworldzycpc.material_preparer.client.config.ItemEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Litematica 文件加载工具
 * 用于从 .litematic 文件中提取物品列表
 */
public class LitematicaLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("MaterialPreparer");

    /**
     * 从 Litematica 文件加载物品列表
     * @param file .litematic 文件
     * @return 物品列表
     * @throws IOException 如果文件读取失败
     */
    public static List<ItemEntry> loadItemListFromLitematica(File file) throws IOException {
        // 读取压缩的 NBT 文件
        CompoundTag root = NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
        
        // 检查是否为有效的 Litematica 文件
        if (!root.contains("Regions")) {
            throw new IOException("Invalid Litematica file: missing Regions tag");
        }
        
        int version = root.getInt("Version");
        LOGGER.info("Loading Litematica file, version: {}", version);
        
        // 获取所有区域
        CompoundTag regions = root.getCompound("Regions");
        if (regions.isEmpty()) {
            throw new IOException("Invalid Litematica file: missing Regions tag");
        }

        // 统计所有方块的数量
        Map<Block, Integer> blockCounts = new HashMap<>();
        
        for (String regionName : regions.getAllKeys()) {
            CompoundTag region = regions.getCompound(regionName);
            if (region.isEmpty()) continue;

            processRegion(region, blockCounts);
        }
        
        // 转换为物品列表
        List<ItemEntry> itemList = getItemEntries(blockCounts);

        // 按物品 ID 排序
        itemList.sort(Comparator.comparing(a -> BuiltInRegistries.ITEM.getKey(a.item())));
        
        LOGGER.info("Loaded {} item types from Litematica file", itemList.size());
        
        return itemList;
    }

    private static @NotNull List<ItemEntry> getItemEntries(Map<Block, Integer> blockCounts) {
        List<ItemEntry> itemList = new ArrayList<>();

        for (Map.Entry<Block, Integer> entry : blockCounts.entrySet()) {
            Block block = entry.getKey();
            int count = entry.getValue();

            // 跳过空气
            if (block == Blocks.AIR) continue;

            // 获取对应的物品
            Item item = block.asItem();

            // 跳过没有物品形式的方块
            if (item == Items.AIR) continue;

            itemList.add(new ItemEntry(item, count));
        }
        return itemList;
    }

    /**
     * 处理一个区域，统计方块数量
     */
    private static void processRegion(CompoundTag region, Map<Block, Integer> blockCounts) {
        // 获取方块状态调色板
        ListTag palette = region.getList("BlockStatePalette", 10); // 10 = compound
        if (palette.isEmpty()) {
            LOGGER.warn("Region has empty BlockStatePalette, skipping");
            return;
        }
        
        // 解析调色板
        Block[] blocks = new Block[palette.size()];
        for (int i = 0; i < palette.size(); i++) {
            CompoundTag entry = palette.getCompound(i);
            String name = entry.getString("Name");
            ResourceLocation id = ResourceLocation.tryParse(name);
            if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
                blocks[i] = BuiltInRegistries.BLOCK.get(id);
            } else {
                blocks[i] = Blocks.AIR;
            }
        }
        
        // 获取 BlockStates 数组
        long[] blockStates = null;
        if (region.contains("BlockStates")) {
            LongArrayTag longArray = (LongArrayTag) region.get("BlockStates");
            if (longArray != null) {
                blockStates = longArray.getAsLongArray();
            }
        }
        
        if (blockStates == null || blockStates.length == 0) {
            LOGGER.warn("Region has empty BlockStates, skipping");
            return;
        }
        
        // 计算每个索引需要的位数
        int bits = 1;
        while ((1 << bits) < blocks.length) {
            bits++;
        }
        
        // 获取区域大小
        int width = 1, height = 1, length = 1;
        if (region.contains("Size")) {
            CompoundTag size = region.getCompound("Size");
            width = Math.abs(size.getInt("x"));
            height = Math.abs(size.getInt("y"));
            length = Math.abs(size.getInt("z"));
        }
        
        int totalBlocks = width * height * length;
        
        // 解包 BlockStates 数组并统计
        int mask = (1 << bits) - 1;
        int currentBit = 0;
        int longIndex = 0;
        long currentLong = blockStates[0];
        
        // 转换为无符号 long
        // Java 中 long 是有符号的，需要处理
        // 不过，位运算时会自动处理
        
        for (int i = 0; i < totalBlocks; i++) {
            int index;
            
            if (currentBit + bits > 64) {
                // 索引跨越了 long 边界
                int remainingBits = 64 - currentBit;
                long part1 = (currentLong >>> currentBit) & ((1L << remainingBits) - 1);
                
                longIndex++;
                if (longIndex >= blockStates.length) break;
                currentLong = blockStates[longIndex];
                
                long part2 = currentLong & ((1L << (bits - remainingBits)) - 1);
                
                index = (int) (part1 | (part2 << remainingBits));
                currentBit = bits - remainingBits;
            } else {
                index = (int) ((currentLong >>> currentBit) & mask);
                currentBit += bits;
            }
            
            if (index >= 0 && index < blocks.length) {
                Block block = blocks[index];
                blockCounts.put(block, blockCounts.getOrDefault(block, 0) + 1);
            }
        }
    }
}
