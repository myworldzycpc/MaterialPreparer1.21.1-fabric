package io.github.myworldzycpc.material_preparer.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import io.github.myworldzycpc.material_preparer.client.config.ItemEntry;
import io.github.myworldzycpc.material_preparer.client.config.ItemEntryController;
import io.github.myworldzycpc.material_preparer.client.config.MaterialPreparerConfig;
import io.github.myworldzycpc.material_preparer.client.keybind.ActionKeybindController;
import io.github.myworldzycpc.material_preparer.client.keybind.KeybindHelper;
import io.github.myworldzycpc.material_preparer.client.keybind.TickBoxKeybindController;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;

import static io.github.myworldzycpc.material_preparer.client.EventHandler.startItemCollection;
import static io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient.*;
import static io.github.myworldzycpc.material_preparer.client.config.MaterialPreparerConfig.HANDLER;

public class ModMenuIntegration implements ModMenuApi {

    public static Screen getModConfigScreen(Screen parentScreen) {
        Screen screen = mc.screen;
        int maxWidth;
        int maxHeight;
        if (screen != null) {
            maxWidth = screen.width;
            maxHeight = screen.height;
        } else {
            maxWidth = 500;
            maxHeight = 500;
        }
        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("Material Preparer Config"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("General"))
                        .tooltip(Component.translatable("General settings"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("General"))
                                .description(OptionDescription.of(Component.translatable("General settings")))
                                .option(KeybindHelper.actionOption("Open Nearest Crafting Table", "Open the nearest crafting table by right-clicking on a crafting table", craftingTableKeybind, true))
                                .option(KeybindHelper.actionOption("Open Screen", "Open this screen", openScreenKeybind, false))
                                .option(KeybindHelper.actionOption("Explore All Nearby Containers", "Explore all nearby containers", exploreAllNearbyContainerKeybind, true))
                                .option(KeybindHelper.actionOption("Collect Items", "Collect items from nearby containers (according to the item list)", collectItemsKeybind, true))
                                .build()
                        )
                        .build()
                )
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("Item List"))
                        .tooltip(Component.translatable("Item list"))
                        .option(KeybindHelper.actionOption("Load Item List from Litematica", "Load item list from Litematica (.litematica) file", loadItemListFromLitematicaKeybind, false))
                        .option(KeybindHelper.actionOption("Load Item List from CSV", "Load item list from CSV file", loadItemListFromCSVKeybind, false))
                        .option(ListOption.<ItemEntry>createBuilder()
                                .name(Component.translatable("Item List"))
                                .description(OptionDescription.of(Component.translatable("List of items to collect")))
                                .binding(Binding.generic(new ArrayList<>(), () -> itemList, newVal -> itemList = newVal))
                                .customController(opt -> new ItemEntryController(opt))
                                .initial(new ItemEntry(Items.STONE, 10))
                                .build()
                        )
                        .build()
                )
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("Debugging"))
                        .tooltip(Component.translatable("Debugging settings"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("Rendering"))
                                .description(OptionDescription.of(Component.translatable("Debug rendering, such as borders")))
                                .option(KeybindHelper.tickBoxOption("Show Debugging Borders", "Show borders for debugging purposes, such as Chests", bordersKeybind, () -> config.showDebuggingBorders, newVal -> config.showDebuggingBorders = newVal))
                                .build()
                        )
                        .build()
                )
                .save(HANDLER::save);
        return builder.build().generateScreen(parentScreen);
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModMenuIntegration::getModConfigScreen;
    }
}
