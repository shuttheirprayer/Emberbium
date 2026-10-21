package org.embeddedt.embeddium.gui.frame.components;

import com.mojang.blaze3d.platform.InputConstants;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;
import org.embeddedt.embeddium.gui.animation.Animation;

import java.util.function.Consumer;

public class ScrollBarComponent extends AbstractWidget {

    protected static final int SCROLL_OFFSET = 6;

    protected final Dim2i dim;

    private final Mode mode;
    private final int frameLength;
    private final int viewPortLength;
    private final int maxScrollBarOffset;
    private final Consumer<Integer> onSetOffset;
    private int offset = 0;
    private boolean isDragging;

    private final Animation scrollEase = new Animation(45.0f, 0.0f);
    private final Animation thumbHighlight = new Animation(60.0f, 0.0f);

    private Dim2i scrollThumb = null;
    private int scrollThumbClickOffset;

    private Dim2i extendedScrollArea = null;

    public ScrollBarComponent(Dim2i trackArea, Mode mode, int frameLength, int viewPortLength, Consumer<Integer> onSetOffset) {
        this.dim = trackArea;
        this.mode = mode;
        this.frameLength = frameLength;
        this.viewPortLength = viewPortLength;
        this.onSetOffset = onSetOffset;
        this.maxScrollBarOffset = this.frameLength - this.viewPortLength;
    }

    public ScrollBarComponent(Dim2i scrollBarArea, Mode mode, int frameLength, int viewPortLength, Consumer<Integer> onSetOffset, Dim2i extendedTrackArea) {
        this(scrollBarArea, mode, frameLength, viewPortLength, onSetOffset);
        this.extendedScrollArea = extendedTrackArea;
    }

    public void updateThumbPosition() {
        this.updateThumbPosition(this.offset);
    }

    private void updateThumbPosition(float offset) {
        int scrollThumbLength = (this.viewPortLength * (this.mode == Mode.VERTICAL ? this.dim.height() : this.dim.width() - 6)) / this.frameLength;
        int maximumScrollThumbOffset = this.viewPortLength - scrollThumbLength;
        int scrollThumbOffset = Math.round(offset * maximumScrollThumbOffset / (float) this.maxScrollBarOffset);
        this.scrollThumb = new Dim2i(this.dim.x() + 2 + (this.mode == Mode.HORIZONTAL ? scrollThumbOffset : 0), this.dim.y() + 2 + (this.mode == Mode.VERTICAL ? scrollThumbOffset : 0), (this.mode == Mode.VERTICAL ? this.dim.width() : scrollThumbLength) - 4, (this.mode == Mode.VERTICAL ? scrollThumbLength : this.dim.height()) - 4);
    }

    public float getRenderOffset() {
        this.scrollEase.setTarget(this.offset);
        return this.scrollEase.get();
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.updateThumbPosition(this.getRenderOffset());

        float highlight = this.thumbHighlight.towards(this.isDragging || this.scrollThumb.containsCursor(mouseX, mouseY));

        this.drawBorder(drawContext, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), 0xFFAAAAAA);
        this.drawRect(drawContext, this.scrollThumb.x(), this.scrollThumb.y(), this.scrollThumb.getLimitX(), this.scrollThumb.getLimitY(), Animation.lerpColor(0xFFAAAAAA, 0xFFFFFFFF, highlight));
        if (this.isFocused()) {
            this.drawBorder(drawContext, this.dim.x(), this.dim.y(), this.dim.getLimitX(), this.dim.getLimitY(), -1);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.dim.containsCursor(mouseX, mouseY)) {
            if (this.scrollThumb.containsCursor(mouseX, mouseY)) {
                if (this.mode == Mode.VERTICAL) {
                    this.scrollThumbClickOffset = (int) (mouseY - (this.scrollThumb.y() + this.scrollThumb.height() / 2));
                } else {
                    this.scrollThumbClickOffset = (int) (mouseX - (this.scrollThumb.x() + this.scrollThumb.width() / 2));
                }
                this.isDragging = true;
            } else {
                int value;
                if (this.mode == Mode.VERTICAL) {
                    value = (int) ((mouseY - this.dim.y() - (this.scrollThumb.height() / 2)) / (this.dim.height() - this.scrollThumb.height()) * this.maxScrollBarOffset);
                } else {
                    value = (int) ((mouseX - this.dim.x() - (this.scrollThumb.width() / 2)) / (this.dim.width() - this.scrollThumb.width()) * this.maxScrollBarOffset);
                }
                this.setOffset(value);
                this.isDragging = false;
            }
            return true;
        }
        this.isDragging = false;
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDragging = false;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.isDragging) {
            int value;
            if (this.mode == Mode.VERTICAL) {
                value = (int) ((mouseY - this.scrollThumbClickOffset - this.dim.y() - (this.scrollThumb.height() / 2)) / (this.dim.height() - this.scrollThumb.height()) * this.maxScrollBarOffset);
            } else {
                value = (int) ((mouseX - this.scrollThumbClickOffset - this.dim.x() - (this.scrollThumb.width() / 2)) / (this.dim.width() - this.scrollThumb.width()) * this.maxScrollBarOffset);
            }
            this.setOffset(value);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        if (this.dim.containsCursor(mouseX, mouseY) || this.extendedScrollArea != null && this.extendedScrollArea.containsCursor(mouseX, mouseY)) {
            if (this.offset <= this.maxScrollBarOffset && this.offset >= 0) {
                int value = (int) (this.offset - verticalAmount * SCROLL_OFFSET); // todo: horizontal separation
                this.setOffset(value);
                return true;
            }
        }
        return false;
    }

    public int getOffset() {
        return this.offset;
    }

    public void setOffset(int value) {
        this.offset = Mth.clamp(value, 0, this.maxScrollBarOffset);

        if (this.isDragging) {
            this.scrollEase.snapTo(this.offset);
        }

        this.updateThumbPosition();
        this.onSetOffset.accept(this.offset);
    }

    @Override
    public ScreenRectangle getRectangle() {
        return new ScreenRectangle(this.dim.x(), this.dim.y(), this.dim.width(), this.dim.height());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isFocused())
            return false;

        if (this.mode == Mode.VERTICAL) {
            if (keyCode == InputConstants.KEY_UP) {
                this.setOffset(this.getOffset() - SCROLL_OFFSET);
                return true;
            } else if (keyCode == InputConstants.KEY_DOWN) {
                this.setOffset(this.getOffset() + SCROLL_OFFSET);
                return true;
            }
        } else {
            if (keyCode == InputConstants.KEY_LEFT) {
                this.setOffset(this.getOffset() - SCROLL_OFFSET);
                return true;
            } else if (keyCode == InputConstants.KEY_RIGHT) {
                this.setOffset(this.getOffset() + SCROLL_OFFSET);
                return true;
            }
        }

        return false;
    }

    public enum Mode {
        HORIZONTAL,
        VERTICAL
    }
}