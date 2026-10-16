package me.jellysquid.mods.sodium.mixin.features.textures.animations.tracking;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.texture.SpriteContentsExtended;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SpriteContents.Ticker.class)
public class SpriteContentsAnimatorImplMixin {
    @Shadow
    int subFrame;
    @Shadow
    int frame;

    @Unique
    private SpriteContents parent;

    @Unique
    private int[] frameTimes;

    /**
     * @author IMS
     * @reason Replace fragile Shadow
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(SpriteContents spriteContents, SpriteContents.AnimatedTexture animation, SpriteContents.InterpolationData interpolation, CallbackInfo ci) {
        this.parent = spriteContents;

        List<SpriteContents.FrameInfo> frames = ((SpriteContentsAnimationAccessor) animation).getFrames();
        int[] frameTimes = new int[frames.size()];

        for (int i = 0; i < frameTimes.length; i++) {
            frameTimes[i] = ((SpriteContentsAnimationFrameAccessor) frames.get(i)).getTime();
        }

        this.frameTimes = frameTimes;
    }

    @Inject(method = "tickAndUpload", at = @At("HEAD"), cancellable = true)
    private void preTick(CallbackInfo ci) {
        SpriteContentsExtended parent = (SpriteContentsExtended) this.parent;

        boolean onDemand = SodiumClientMod.options().performance.animateOnlyVisibleTextures;

        if (onDemand && !parent.sodium$isActive()) {
            int[] frameTimes = this.frameTimes;
            this.subFrame++;
            if (this.subFrame >= frameTimes[this.frame]) {
                this.frame = (this.frame + 1) % frameTimes.length;
                this.subFrame = 0;
            }
            ci.cancel();
        }
    }

    @Inject(method = "tickAndUpload", at = @At("TAIL"))
    private void postTick(CallbackInfo ci) {
        SpriteContentsExtended parent = (SpriteContentsExtended) this.parent;
        parent.sodium$setActive(false);
    }
}
