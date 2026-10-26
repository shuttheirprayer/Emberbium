package me.jellysquid.mods.sodium.mixin.core.render.world;

import me.jellysquid.mods.sodium.client.render.BlockEntityRenderBoxKind;
import net.minecraft.world.level.block.entity.BlockEntity;
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
}
