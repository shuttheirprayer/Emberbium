package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.GlObject;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL45C;

public abstract class GlBuffer extends GlObject {
    private GlBufferMapping activeMapping;

    protected GlBuffer() {
        // Use glCreateBuffers on GL 4.5+ for DSA compatibility.
        // glGenBuffers only reserves a name; glCreateBuffers creates a real buffer object.
        if (GL.getCapabilities().OpenGL45 && SodiumClientMod.options().advanced.useModernGlPaths) {
            this.setHandle(GL45C.glCreateBuffers());
        } else {
            this.setHandle(GL20C.glGenBuffers());
        }
    }

    public GlBufferMapping getActiveMapping() {
        return this.activeMapping;
    }

    public void setActiveMapping(GlBufferMapping mapping) {
        this.activeMapping = mapping;
    }
}
