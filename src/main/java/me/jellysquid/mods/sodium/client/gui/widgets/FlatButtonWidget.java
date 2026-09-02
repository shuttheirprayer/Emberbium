package me.jellysquid.mods.sodium.client.gui.widgets;

import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.gui.animation.Animation;
import org.embeddedt.embeddium.gui.theme.DefaultColors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class FlatButtonWidget extends AbstractWidget implements Renderable {
    protected final Dim2i dim;
    private final Runnable action;

    private @NotNull Style style = Style.defaults();

    private boolean selected;
    private boolean enabled = true;
    private boolean visible = true;
    private boolean leftAligned;

    private Component label;

    private final Animation hoverFade = new Animation(55.0f, 0.0f);
    private final Animation selectGrow = new Animation(70.0f, 0.0f);

    public FlatButtonWidget(Dim2i dim, Component label, Runnable action) {
        this.dim = dim;
        this.label = label;
        this.action = action;
    }

    protected int getLeftAlignedTextOffset() {
        return 10;
    }

    protected boolean isHovered(int mouseX, int mouseY) {
        return this.dim.containsCursor(mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }

        this.hovered = this.isHovered(mouseX, mouseY);

        float hover = this.hoverFade.towards(this.enabled && this.hovered);

        int backgroundColor = this.enabled
                ? Animation.lerpColor(this.style.bgDefault, this.style.bgHovered, hover)
                : this.style.bgDisabled;
        int textColor = this.enabled ? this.style.textDefault : this.style.textDisabled;

        int strWidth = this.font.width(this.label);

        this.drawRect(drawContext, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), backgroundColor);
        int textX;
        if (this.leftAligned) {
            textX = this.dim.x() + this.getLeftAlignedTextOffset();
        } else {
            textX = this.dim.getCenterX() - (strWidth / 2);
        }
        this.drawString(drawContext, this.label, textX, this.dim.getCenterY() - 4, textColor);

        // The selection marker grows out from the middle of the edge it sits on, rather than
        // appearing whole. Kept on the same edges and at the same thickness as before.
        float selectedFade = this.selectGrow.towards(this.enabled && this.selected);

        if (selectedFade > 0.0f) {
            int markerColor = Animation.scaleAlpha(DefaultColors.ELEMENT_ACTIVATED, Math.min(1.0f, selectedFade * 2.0f));
            float grow = Animation.smoothstep(selectedFade);

            if (this.leftAligned) {
                int half = Math.round((this.dim.height() / 2.0f) * grow);
                int center = this.dim.getCenterY();
                this.drawRect(drawContext, this.dim.x(), center - half, this.dim.x() + 1, center + half, markerColor);
            } else {
                int half = Math.round((this.dim.width() / 2.0f) * grow);
                int center = this.dim.getCenterX();
                this.drawRect(drawContext, center - half, this.dim.getLimitY() - 1, center + half, this.dim.getLimitY(), markerColor);
            }
        }
        if (this.enabled && this.isFocused()) {
            this.drawBorder(drawContext, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), -1);
        }
    }

    public void setStyle(@NotNull Style style) {
        Objects.requireNonNull(style);

        this.style = style;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setLeftAligned(boolean leftAligned) {
        this.leftAligned = leftAligned;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.enabled || !this.visible) {
            return false;
        }

        if (button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
            doAction();

            return true;
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isFocused())
            return false;

        if (CommonInputs.selected(keyCode)) {
            doAction();
            return true;
        }

        return false;
    }

    private void doAction() {
        this.action.run();
        this.playClickSound();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setLabel(Component text) {
        this.label = text;
    }

    public Component getLabel() {
        return this.label;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigation) {
        if (!this.enabled || !this.visible)
            return null;
        return super.nextFocusPath(navigation);
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height());
    }

    public Dim2i getDimensions() {
        return this.dim;
    }

    public static class Style {
        public int bgHovered, bgDefault, bgDisabled;
        public int textDefault, textDisabled;

        public static Style defaults() {
            var style = new Style();
            style.bgHovered = 0xE0202020;
            style.bgDefault = 0x90000000;
            style.bgDisabled = 0x60000000;
            style.textDefault = 0xFFFFFFFF;
            style.textDisabled = 0x90FFFFFF;

            return style;
        }
    }
}
