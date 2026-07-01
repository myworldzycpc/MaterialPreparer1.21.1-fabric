package io.github.myworldzycpc.material_preparer.client.config;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.utils.Dimension;
import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ControllerWidget;
import dev.isxander.yacl3.gui.utils.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public abstract class SubActionElement<T extends Controller<?>> extends ControllerWidget<T> {


    public SubActionElement(T control, YACLScreen screen, Dimension<Integer> dim) {
        super(control, screen, dim);
    }

    @Override
    protected int getHoveredControlWidth() {
        return 0;
    }

    public abstract Component getSubActionText();

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        hovered = isMouseOver(mouseX, mouseY);

        Component name = control.option().changed() ? modifiedOptionName : control.option().name();
        Component shortenedName = Component.literal(GuiUtils.shortenString(name.getString(), textRenderer, getDimension().width() - getControlWidth() - getXPadding() - 7, "...")).setStyle(name.getStyle());

        drawButtonRect(graphics, getDimension().x(), getDimension().y(), getDimension().xLimit() - getSubActionWidth(), getDimension().yLimit(), isBodyHovered(mouseX, mouseY), isAvailable());
        drawButtonRect(graphics, getDimension().xLimit() - getSubActionWidth(), getDimension().y(), getDimension().xLimit(), getDimension().yLimit(), isSubActionHovered(mouseX, mouseY), isAvailable());
        graphics.drawString(textRenderer, shortenedName, getDimension().x() + getXPadding(), getTextY(), getValueColor(), true);

        drawValueText(graphics, mouseX, mouseY, delta);
        if (isHovered()) {
            drawHoveredControl(graphics, mouseX, mouseY, delta);
        }

        graphics.drawCenteredString(textRenderer, getSubActionText(), getDimension().xLimit() - getSubActionWidth() / 2, getTextY(), -1);
    }

    public boolean isBodyHovered(int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY) || !isAvailable()) return false;
        return !isSubActionHovered(mouseX, mouseY);
    }

    public boolean isSubActionHovered(int mouseX, int mouseY) {
        if (!isMouseOver(mouseX, mouseY) || !isAvailable()) return false;
        return mouseX >= getDimension().xLimit() - getSubActionWidth();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY) || !isAvailable()) return false;
        if (mouseX >= getDimension().xLimit() - getSubActionWidth()) {
            mouseClickedOnSubAction(mouseX, mouseY, button);
            playDownSound();
            return true;
        }
        return mouseClickedOnBody(mouseX, mouseY, button);
    }

    public boolean mouseClickedOnBody(double mouseX, double mouseY, int button) {
        return true;
    }

    public boolean mouseClickedOnSubAction(double mouseX, double mouseY, int button) {
        return true;
    }

    public int getSubActionWidth() {
        return 20;
    }

    @Override
    protected void drawValueText(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Component valueText = getValueText();
        graphics.drawString(textRenderer, valueText, getDimension().xLimit() - textRenderer.width(valueText) - getXPadding() - getSubActionWidth(), getTextY(), getValueColor(), true);
    }
}
