package me.jellysquid.mods.sodium.mixin.features.shader.uniform;

import com.mojang.blaze3d.shaders.Uniform;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.UniformSnapshot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@Mixin(Uniform.class)
public class UniformMixin implements UniformSnapshot {
    @Shadow
    private int location;

    @Shadow
    @Final
    private int count;

    @Shadow
    @Final
    private int type;

    @Shadow
    @Final
    private IntBuffer intValues;

    @Shadow
    @Final
    private FloatBuffer floatValues;

    @Unique
    private int[] sodium$lastUpload;

    @Unique
    private int sodium$lastLocation = -1;

    @Inject(method = "upload", at = @At("HEAD"))
    private void sodium$recordUpload(CallbackInfo ci) {
        int count = this.count;
        int[] last = this.sodium$lastUpload;

        if (last == null || last.length != count) {
            last = this.sodium$lastUpload = new int[count];
        }

        if (this.type <= 3) {
            IntBuffer values = this.intValues;

            for (int i = 0; i < count; i++) {
                last[i] = values.get(i);
            }
        } else {
            FloatBuffer values = this.floatValues;

            for (int i = 0; i < count; i++) {
                last[i] = Float.floatToRawIntBits(values.get(i));
            }
        }

        this.sodium$lastLocation = this.location;
    }

    @Override
    public boolean sodium$matchesLastUpload() {
        int[] last = this.sodium$lastUpload;

        if (last == null || this.sodium$lastLocation != this.location || this.location == -1) {
            return false;
        }

        int count = this.count;

        if (this.type <= 3) {
            IntBuffer values = this.intValues;

            for (int i = 0; i < count; i++) {
                if (last[i] != values.get(i)) {
                    return false;
                }
            }
        } else {
            FloatBuffer values = this.floatValues;

            for (int i = 0; i < count; i++) {
                if (last[i] != Float.floatToRawIntBits(values.get(i))) {
                    return false;
                }
            }
        }

        return true;
    }
}
