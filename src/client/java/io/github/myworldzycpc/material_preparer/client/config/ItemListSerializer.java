package io.github.myworldzycpc.material_preparer.client.config;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemListSerializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("MaterialPreparer");
    private static final String CSV_HEADER = "item_id,count";

    public static List<ItemEntry> loadFromCsv(Path filePath) {
        List<ItemEntry> itemList = new ArrayList<>();

        if (!Files.exists(filePath)) {
            LOGGER.info("Item list CSV file not found, starting with empty list: {}", filePath);
            return itemList;
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            // Skip header if present
            if (line != null && line.equalsIgnoreCase(CSV_HEADER)) {
                line = reader.readLine();
            }

            while (line != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split(",");
                    if (parts.length >= 2) {
                        String itemId = parts[0].trim();
                        try {
                            int count = Integer.parseInt(parts[1].trim());
                            Optional.ofNullable(Identifier.tryParse(itemId))
                                    .flatMap(BuiltInRegistries.ITEM::get)
                                    .map(Holder.Reference::value)
                                    .filter(item -> item != Items.AIR)
                                    .ifPresentOrElse(
                                            item -> itemList.add(new ItemEntry(item, count)),
                                            () -> LOGGER.warn("Unknown item ID: {}", itemId)
                                    );
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Invalid count value for item {}: {}", itemId, parts[1]);
                        }
                    }
                }
                line = reader.readLine();
            }

            LOGGER.info("Loaded {} items from CSV: {}", itemList.size(), filePath);
        } catch (IOException e) {
            LOGGER.error("Failed to load item list from CSV: {}", filePath, e);
        }

        return itemList;
    }

    public static void saveToCsv(List<ItemEntry> itemList, Path filePath) {
        try {
            // Ensure parent directory exists
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
                writer.write(CSV_HEADER);
                writer.newLine();

                for (ItemEntry entry : itemList) {
                    String itemId = BuiltInRegistries.ITEM.getKey(entry.item()).toString();
                    writer.write(itemId + "," + entry.count());
                    writer.newLine();
                }
            }

            LOGGER.info("Saved {} items to CSV: {}", itemList.size(), filePath);
        } catch (IOException e) {
            LOGGER.error("Failed to save item list to CSV: {}", filePath, e);
        }
    }
}
