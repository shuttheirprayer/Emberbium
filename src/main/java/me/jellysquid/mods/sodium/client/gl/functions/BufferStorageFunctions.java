package me.jellysquid.mods.sodium.client.gl.functions;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferStorageFlags;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTarget;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.util.EnumBitField;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL44C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.opengl.GLCapabilities;

public enum BufferStorageFunctions {
    NONE {
        @Override
        public void createBufferStorage(GlBufferTarget target, long length, EnumBitField<GlBufferStorageFlags> flags) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void createBufferStorage(int buffer, long length, EnumBitField<GlBufferStorageFlags> flags) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDSA() {
            return false;
        }
    },
    DSA {
        @Override
        public void createBufferStorage(GlBufferTarget target, long length, EnumBitField<GlBufferStorageFlags> flags) {
            GL44C.glBufferStorage(target.getTargetParameter(), length, flags.getBitField());
        }

        @Override
        public void createBufferStorage(int buffer, long length, EnumBitField<GlBufferStorageFlags> flags) {
            GL45C.glNamedBufferStorage(buffer, length, flags.getBitField());
        }

        @Override
        public boolean isDSA() {
            return true;
        }
    },
    CORE {
        @Override
        public void createBufferStorage(GlBufferTarget target, long length, EnumBitField<GlBufferStorageFlags> flags) {
            GL44C.glBufferStorage(target.getTargetParameter(), length, flags.getBitField());
        }

        @Override
        public void createBufferStorage(int buffer, long length, EnumBitField<GlBufferStorageFlags> flags) {
            GL44C.glBindBuffer(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), buffer);
            GL44C.glBufferStorage(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), length, flags.getBitField());
        }

        @Override
        public boolean isDSA() {
            return false;
        }
    },
    ARB {
        @Override
        public void createBufferStorage(GlBufferTarget target, long length, EnumBitField<GlBufferStorageFlags> flags) {
            ARBBufferStorage.glBufferStorage(target.getTargetParameter(), length, flags.getBitField());
        }

        @Override
        public void createBufferStorage(int buffer, long length, EnumBitField<GlBufferStorageFlags> flags) {
            GL20C.glBindBuffer(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), buffer);
            ARBBufferStorage.glBufferStorage(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), length, flags.getBitField());
        }

        @Override
        public boolean isDSA() {
            return false;
        }
    };

    public static BufferStorageFunctions pickBest(RenderDevice device) {
        GLCapabilities capabilities = device.getCapabilities();

        boolean dsa = SodiumClientMod.canUseModernGlPaths();

        if (dsa && capabilities.OpenGL45) {
            return DSA;
        } else if (capabilities.OpenGL44) {
            return CORE;
        } else if (capabilities.GL_ARB_buffer_storage) {
            return ARB;
        } else {
            return NONE;
        }
    }

    public abstract void createBufferStorage(GlBufferTarget target, long length, EnumBitField<GlBufferStorageFlags> flags);

    public abstract void createBufferStorage(int buffer, long length, EnumBitField<GlBufferStorageFlags> flags);

    public abstract boolean isDSA();
}
