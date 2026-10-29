package me.jellysquid.mods.sodium.mixin.core.render.world;

import me.jellysquid.mods.sodium.client.render.BlockEntityRenderBoxKind;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockEntity.class)
public class BlockEntityRenderBoxKindMixin implements BlockEntityRenderBoxKind {
    @Unique
    private byte embeddium$renderBoxKind;

    @Override
    public byte embeddium$renderBoxKind() {
        return this.embeddium$renderBoxKind;
    }

    @Override
    public void embeddium$setRenderBoxKind(byte kind) {
        this.embeddium$renderBoxKind = kind;
    }

    @Unique
    private BlockState embeddium$boxState;

    @Unique
    private double[] embeddium$box;

    @Override
    public BlockState embeddium$boxState() {
        return this.embeddium$boxState;
    }

    @Override
    public double[] embeddium$box() {
        return this.embeddium$box;
    }

    @Override
    public void embeddium$setBox(BlockState state, double[] box) {
        this.embeddium$boxState = state;
        this.embeddium$box = box;
    }
}
