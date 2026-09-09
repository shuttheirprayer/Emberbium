package me.jellysquid.mods.sodium.mixin.features.textures.animations.tracking;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextureAtlas.class)
public class SpriteAtlasTextureMixin {
    @ModifyReturnValue(method = "getSprite", at = @At("RETURN"))
    private TextureAtlasSprite preReturnSprite(TextureAtlasSprite sprite) {
        SpriteUtil.markSpriteActive(sprite);
        return sprite;
    }
}
