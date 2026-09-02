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
    /**
     * GL 4.5+ Direct State Access path for buffer storage creation.
     */
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
            // Fallback: bind then create storage
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
            // Fallback: bind then create storage
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

        // Emberbium: with modern paths off, fall through to the pre-existing non-DSA ladder.
        boolean dsa = SodiumClientMod.options().advanced.useModernGlPaths;

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

    /**
     * Direct State Access variant - creates buffer storage without binding.
     * On non-DSA paths, this will bind the buffer first.
     */
    public abstract void createBufferStorage(int buffer, long length, EnumBitField<GlBufferStorageFlags> flags);

    public abstract boolean isDSA();
}
