package me.jellysquid.mods.sodium.client.gl.functions;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferMapFlags;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTarget;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferUsage;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.util.EnumBitField;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL31C;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.opengl.GLCapabilities;

import java.nio.ByteBuffer;

/**
 * Abstraction layer for buffer data operations.
 * Uses Direct State Access (DSA) on GL 4.5+ for reduced binding overhead,
 * falls back to traditional bind-then-operate on older GL versions.
 */
public enum BufferDataFunctions {
    /**
     * GL 4.5+ Direct State Access path.
     * Operates directly on buffer handles without binding.
     */
    DSA {
        @Override
        public void bufferData(int buffer, ByteBuffer data, GlBufferUsage usage) {
            GL45C.glNamedBufferData(buffer, data, usage.getId());
        }

        @Override
        public void bufferData(int buffer, long size, GlBufferUsage usage) {
            GL45C.glNamedBufferData(buffer, size, usage.getId());
        }

        @Override
        public void bufferSubData(int buffer, long offset, ByteBuffer data) {
            GL45C.glNamedBufferSubData(buffer, offset, data);
        }

        @Override
        public void copyBufferSubData(int srcBuffer, int dstBuffer, long readOffset, long writeOffset, long size) {
            GL45C.glCopyNamedBufferSubData(srcBuffer, dstBuffer, readOffset, writeOffset, size);
        }

        @Override
        public ByteBuffer mapBufferRange(int buffer, long offset, long length, EnumBitField<GlBufferMapFlags> flags) {
            return GL45C.glMapNamedBufferRange(buffer, offset, length, flags.getBitField());
        }

        @Override
        public void flushMappedBufferRange(int buffer, long offset, long length) {
            GL45C.glFlushMappedNamedBufferRange(buffer, offset, length);
        }

        @Override
        public boolean unmapBuffer(int buffer) {
            return GL45C.glUnmapNamedBuffer(buffer);
        }

        @Override
        public boolean isDSA() {
            return true;
        }
    },

    /**
     * Legacy bind-then-operate path for GL < 4.5.
     * Requires buffer to be bound before operations.
     */
    BINDING {
        @Override
        public void bufferData(int buffer, ByteBuffer data, GlBufferUsage usage) {
            GL20C.glBindBuffer(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), buffer);
            GL20C.glBufferData(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), data, usage.getId());
        }

        @Override
        public void bufferData(int buffer, long size, GlBufferUsage usage) {
            GL20C.glBindBuffer(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), buffer);
            GL20C.glBufferData(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), size, usage.getId());
        }

        @Override
        public void bufferSubData(int buffer, long offset, ByteBuffer data) {
            GL20C.glBindBuffer(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), buffer);
            GL20C.glBufferSubData(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), offset, data);
        }

        @Override
        public void copyBufferSubData(int srcBuffer, int dstBuffer, long readOffset, long writeOffset, long size) {
            GL20C.glBindBuffer(GL31C.GL_COPY_READ_BUFFER, srcBuffer);
            GL20C.glBindBuffer(GL31C.GL_COPY_WRITE_BUFFER, dstBuffer);
            GL31C.glCopyBufferSubData(GL31C.GL_COPY_READ_BUFFER, GL31C.GL_COPY_WRITE_BUFFER, readOffset, writeOffset, size);
        }

        @Override
        public ByteBuffer mapBufferRange(int buffer, long offset, long length, EnumBitField<GlBufferMapFlags> flags) {
            GL20C.glBindBuffer(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), buffer);
            return GL32C.glMapBufferRange(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), offset, length, flags.getBitField());
        }

        @Override
        public void flushMappedBufferRange(int buffer, long offset, long length) {
            GL20C.glBindBuffer(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), buffer);
            GL32C.glFlushMappedBufferRange(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), offset, length);
        }

        @Override
        public boolean unmapBuffer(int buffer) {
            GL20C.glBindBuffer(GlBufferTarget.ARRAY_BUFFER.getTargetParameter(), buffer);
            return GL32C.glUnmapBuffer(GlBufferTarget.ARRAY_BUFFER.getTargetParameter());
        }

        @Override
        public boolean isDSA() {
            return false;
        }
    };

    public static BufferDataFunctions pickBest(RenderDevice device) {
        // Emberbium: DSA is opt-out. It rewrites every buffer and VAO operation in the
        // renderer, and its benefit is unverified on any given driver, so keep an escape
        // hatch that restores exactly the upstream path.
        if (!SodiumClientMod.options().advanced.useModernGlPaths) {
            return BINDING;
        }

        GLCapabilities capabilities = device.getCapabilities();

        if (capabilities.OpenGL45) {
            return DSA;
        } else {
            return BINDING;
        }
    }

    public abstract void bufferData(int buffer, ByteBuffer data, GlBufferUsage usage);
    public abstract void bufferData(int buffer, long size, GlBufferUsage usage);
    public abstract void bufferSubData(int buffer, long offset, ByteBuffer data);
    public abstract void copyBufferSubData(int srcBuffer, int dstBuffer, long readOffset, long writeOffset, long size);
    public abstract ByteBuffer mapBufferRange(int buffer, long offset, long length, EnumBitField<GlBufferMapFlags> flags);
    public abstract void flushMappedBufferRange(int buffer, long offset, long length);
    public abstract boolean unmapBuffer(int buffer);
    public abstract boolean isDSA();
}
