package io.github.myworldzycpc.material_preparer.client.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import io.github.myworldzycpc.material_preparer.MaterialPreparer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

public class MaterialPreparerConfig {

    public static final ConfigClassHandler<MaterialPreparerConfig> HANDLER = ConfigClassHandler.createBuilder(MaterialPreparerConfig.class)
            .id(ResourceLocation.fromNamespaceAndPath(MaterialPreparer.MOD_ID, "main_config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("material_preparer/config.json5"))
                    .setJson5(true)
                    .build())
            .build();

    @SerialEntry
    public boolean showDebuggingBorders = false;
    @SerialEntry
    public String showDebuggingBordersKeybind = "unbound";

    @SerialEntry
    public String craftingTableKeybind = "unbound";

    @SerialEntry
    public String exploreAllNearbyContainerKeybind = "unbound";

    @SerialEntry
    public String openScreenKeybind = "unbound";

    @SerialEntry
    public String collectItemsKeybind = "unbound";

    @SerialEntry
    public String loadItemListFromLitematicaKeybind = "unbound";

    @SerialEntry
    public String loadItemListFromCSVKeybind = "unbound";

    @SerialEntry
    public Integer minClickInterval = 0;

    @SerialEntry
    public boolean alwaysQuickMove = false;
    @SerialEntry
    public String alwaysQuickMoveKeybind = "unbound";

    @SerialEntry
    public boolean ignoreExistingItems = false;
    @SerialEntry
    public String ignoreExistingItemsKeybind = "unbound";

    @SerialEntry
    public String toggleBlacklistKeybind = "unbound";
    @SerialEntry
    public String toggleOutputContainerKeybind = "unbound";
    @SerialEntry
    public String clearAllMarkersKeybind = "unbound";

    @SerialEntry
    public boolean showDebugMessages = false;
    @SerialEntry
    public String showDebugMessagesKeybind = "unbound";
}
