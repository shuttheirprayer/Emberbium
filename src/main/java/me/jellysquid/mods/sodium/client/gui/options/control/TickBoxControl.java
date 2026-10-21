package me.jellysquid.mods.sodium.client.gui.options.control;

import com.mojang.blaze3d.platform.InputConstants;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.renderer.Rect2i;
import org.embeddedt.embeddium.gui.animation.Animation;
import org.embeddedt.embeddium.gui.theme.DefaultColors;

public class TickBoxControl implements Control<Boolean> {
    private final Option<Boolean> option;

    public TickBoxControl(Option<Boolean> option) {
        this.option = option;
    }

    @Override
    public ControlElement<Boolean> createElement(Dim2i dim) {
        return new TickBoxControlElement(this.option, dim);
    }

    @Override
    public int getMaxWidth() {
        return 30;
    }

    @Override
    public Option<Boolean> getOption() {
        return this.option;
    }

    private static class TickBoxControlElement extends ControlElement<Boolean> {
        private final Rect2i button;

        private final Animation tick = new Animation(50.0f, 0.0f);

        public TickBoxControlElement(Option<Boolean> option, Dim2i dim) {
            super(option, dim);

            this.button = new Rect2i(dim.getLimitX() - 16, dim.getCenterY() - 5, 10, 10);
        }

        @Override
        public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
            super.render(drawContext, mouseX, mouseY, delta);

            final int x = this.button.getX();
            final int y = this.button.getY();
            final int w = x + this.button.getWidth();
            final int h = y + this.button.getHeight();

            final boolean enabled = this.option.isAvailable();
            final boolean ticked = this.option.getValue();

            final float filled = this.tick.towards(ticked);

            final int color;

            if (enabled) {
                color = Animation.lerpColor(0xFFFFFFFF, DefaultColors.ELEMENT_ACTIVATED, filled);
            } else {
                color = 0xFFAAAAAA;
            }

            if (filled > 0.0f) {
                float grow = Animation.smoothstep(filled);
                float centerX = (x + w) / 2.0f;
                float centerY = (y + h) / 2.0f;
                float halfW = ((w - 2) - (x + 2)) / 2.0f * grow;
                float halfH = ((h - 2) - (y + 2)) / 2.0f * grow;

                int fillColor = enabled ? Animation.scaleAlpha(color, Math.min(1.0f, filled * 1.5f)) : color;

                this.drawRect(drawContext,
                        Math.round(centerX - halfW), Math.round(centerY - halfH),
                        Math.round(centerX + halfW), Math.round(centerY + halfH),
                        fillColor);
            }

            this.drawBorder(drawContext, x, y, w, h, color);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.option.isAvailable() && button == 0 && this.dim.containsCursor(mouseX, mouseY)) {
                toggleControl();
                this.playClickSound();

                return true;
            }

            return false;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!isFocused()) return false;

            if (CommonInputs.selected(keyCode)) {
                toggleControl();
                this.playClickSound();

                return true;
            }

            return false;
        }

        public void toggleControl() {
            this.option.setValue(!this.option.getValue());
        }
    }
}
