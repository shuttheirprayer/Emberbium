package me.jellysquid.mods.sodium.client.render;

import net.minecraft.world.level.block.state.BlockState;

public interface BlockEntityRenderBoxKind {
    byte embeddium$renderBoxKind();

    void embeddium$setRenderBoxKind(byte kind);

    BlockState embeddium$boxState();

    double[] embeddium$box();

    void embeddium$setBox(BlockState state, double[] box);
}
