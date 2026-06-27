package io.github.myworldzycpc.material_preparer.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import io.github.myworldzycpc.material_preparer.client.config.ItemEntry;
import io.github.myworldzycpc.material_preparer.client.config.ItemEntryController;
import io.github.myworldzycpc.material_preparer.client.config.ItemListSerializer;
import io.github.myworldzycpc.material_preparer.client.keybind.KeybindHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.nio.file.Path;
import java.util.ArrayList;

import static io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient.*;
import static io.github.myworldzycpc.material_preparer.client.config.MaterialPreparerConfig.HANDLER;

public class ModMenuIntegration implements ModMenuApi {

    public static Screen getModConfigScreen(Screen parentScreen) {
        Screen screen = mc.gui.screen();
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
                .title(Component.translatable("gui.material_preparer.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("gui.material_preparer.category.general"))
                        .tooltip(Component.translatable("gui.material_preparer.category.general.tooltip"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("gui.material_preparer.group.general"))
                                .description(OptionDescription.of(Component.translatable("gui.material_preparer.group.general.desc")))
                                .option(KeybindHelper.actionOption("gui.material_preparer.keybind.crafting_table", "gui.material_preparer.keybind.crafting_table.desc", craftingTableKeybind, true))
                                .option(KeybindHelper.actionOption("gui.material_preparer.keybind.open_screen", "gui.material_preparer.keybind.open_screen.desc", openScreenKeybind, false))
                                .option(KeybindHelper.actionOption("gui.material_preparer.keybind.explore_containers", "gui.material_preparer.keybind.explore_containers.desc", exploreAllNearbyContainerKeybind, true))
                                .option(KeybindHelper.actionOption("gui.material_preparer.keybind.collect_items", "gui.material_preparer.keybind.collect_items.desc", collectItemsKeybind, true))
                                .option(KeybindHelper.actionOption("gui.material_preparer.keybind.stat_cached", "gui.material_preparer.keybind.stat_cached.desc", statCachedItemsKeybind, true))
                                .option(Option.<Integer>createBuilder()
                                        .name(Component.translatable("gui.material_preparer.option.min_click_interval"))
                                        .description(OptionDescription.of(Component.translatable("gui.material_preparer.option.min_click_interval.desc")))
                                        .binding(Binding.generic(0, () -> config.minClickInterval, newVal -> config.minClickInterval = newVal))
                                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                                .range(0, 20)
                                                .step(1)
                                                .formatValue(val -> Component.translatable("gui.material_preparer.format.ticks", val))
                                        )
                                        .build()
                                )
                                .option(KeybindHelper.tickBoxOption("gui.material_preparer.keybind.always_quick_move", "gui.material_preparer.keybind.always_quick_move.desc", alwaysQuickMoveKeybind, () -> config.alwaysQuickMove, newVal -> config.alwaysQuickMove = newVal))
                                .option(KeybindHelper.tickBoxOption("gui.material_preparer.keybind.ignore_existing_items", "gui.material_preparer.keybind.ignore_existing_items.desc", ignoreExistingItemsKeybind, () -> config.ignoreExistingItems, newVal -> config.ignoreExistingItems = newVal))
                                .build()
                        )
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("gui.material_preparer.group.containers"))
                                .description(OptionDescription.of(Component.translatable("gui.material_preparer.group.containers.desc")))
                                .option(KeybindHelper.actionOption("gui.material_preparer.keybind.toggle_blacklist", "gui.material_preparer.keybind.toggle_blacklist.desc", toggleBlacklistKeybind, true))
                                .option(KeybindHelper.actionOption("gui.material_preparer.keybind.toggle_output_container", "gui.material_preparer.keybind.toggle_output_container.desc", toggleOutputContainerKeybind, true))
                                .option(KeybindHelper.actionOption("gui.material_preparer.keybind.clear_all_markers", "gui.material_preparer.keybind.clear_all_markers.desc", clearAllMarkersKeybind, false))
                                .build()
                        )
                        .build()
                )
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("gui.material_preparer.category.item_list"))
                        .tooltip(Component.translatable("gui.material_preparer.category.item_list.tooltip"))
                        .option(KeybindHelper.actionOption("gui.material_preparer.keybind.load_litematica", "gui.material_preparer.keybind.load_litematica.desc", loadItemListFromLitematicaKeybind, true))
                        .option(KeybindHelper.actionOption("gui.material_preparer.keybind.load_csv", "gui.material_preparer.keybind.load_csv.desc", loadItemListFromCSVKeybind, false))
                        .option(ListOption.<ItemEntry>createBuilder()
                                .name(Component.translatable("gui.material_preparer.option.item_list"))
                                .description(OptionDescription.of(Component.translatable("gui.material_preparer.option.item_list.desc")))
                                .binding(Binding.generic(new ArrayList<>(), () -> itemList, newVal -> itemList = newVal))
                                .customController(opt -> new ItemEntryController(opt))
                                .initial(new ItemEntry(Items.STONE, 10))
                                .build()
                        )
                        .build()
                )
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("gui.material_preparer.category.debugging"))
                        .tooltip(Component.translatable("gui.material_preparer.category.debugging.tooltip"))
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("gui.material_preparer.group.rendering"))
                                .description(OptionDescription.of(Component.translatable("gui.material_preparer.group.rendering.desc")))
                                .option(KeybindHelper.tickBoxOption("gui.material_preparer.keybind.debug_borders", "gui.material_preparer.keybind.debug_borders.desc", bordersKeybind, () -> config.showDebuggingBorders, newVal -> config.showDebuggingBorders = newVal))
                                .build()
                        )
                        .group(OptionGroup.createBuilder()
                                .name(Component.translatable("gui.material_preparer.group.messages"))
                                .description(OptionDescription.of(Component.translatable("gui.material_preparer.group.messages.desc")))
                                .option(KeybindHelper.tickBoxOption("gui.material_preparer.keybind.debug_messages", "gui.material_preparer.keybind.debug_messages.desc", showDebugMessagesKeybind, () -> config.showDebugMessages, newVal -> config.showDebugMessages = newVal))
                                .build()
                        )
                        .build()
                )
                .save(() -> {
                    HANDLER.save();
                    saveItemListCsv();
                });
        return builder.build().generateScreen(parentScreen);
    }

    private static void saveItemListCsv() {
        Path csvPath = FabricLoader.getInstance().getConfigDir().resolve("material_preparer/item_list.csv");
        ItemListSerializer.saveToCsv(itemList, csvPath);
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModMenuIntegration::getModConfigScreen;
    }
}
