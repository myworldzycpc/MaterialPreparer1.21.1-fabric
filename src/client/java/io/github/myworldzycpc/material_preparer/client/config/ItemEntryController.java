package io.github.myworldzycpc.material_preparer.client.config;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.ListOptionEntry;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ControllerWidget;
import io.github.myworldzycpc.material_preparer.client.screen.EditItemScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.NonNull;

import static io.github.myworldzycpc.material_preparer.client.MaterialPreparerClient.mc;

public class ItemEntryController implements Controller<ItemEntry> {
    private final ListOptionEntry<ItemEntry> option;

    public ItemEntryController(ListOptionEntry<ItemEntry> option) {
        this.option = option;
    }

    @Override
    public Option<ItemEntry> option() {
        return option;
    }

    @Override
    public Component formatValue() {
        Item item = option.pendingValue().item();
        if (item == null) {
            return Component.translatable("gui.material_preparer.none");
        }
        String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
        try {
            Component displayName = item.getDefaultInstance().getHoverName();
            return Component.translatable("%1$s (%2$s) x %3$s", itemId, displayName, option.pendingValue().count());
        } catch (Exception _) {
            return Component.translatable("%1$s x %2$s", itemId, option.pendingValue().count());
        }
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen screen, Dimension<Integer> widgetDimension) {
        return new ItemEntryElement(this, screen, widgetDimension);
    }

    public static class ItemEntryElement extends ControllerWidget<ItemEntryController> {

        public ItemEntryElement(ItemEntryController control, YACLScreen screen, Dimension<Integer> dim) {
            super(control, screen, dim);
        }

        @Override
        protected int getHoveredControlWidth() {
            return 0;
        }

        @Override
        public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
            double mouseX = event.x();
            double mouseY = event.y();
            if (!isMouseOver(mouseX, mouseY) || !isAvailable())
                return false;
            mc.gui.setScreen(new EditItemScreen(mc.gui.screen(), control.option));
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        protected void extractHoveredControl(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            super.extractHoveredControl(graphics, mouseX, mouseY, a);
            if (this.hovered) {
                graphics.requestCursor(this.isAvailable() ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
            }
        }
    }
}
