package me.jellysquid.mods.sodium.client.gl.functions;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBuffer;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.opengl.GLCapabilities;

/**
 * Abstraction layer for vertex attribute setup.
 * Uses modern separate attribute format (GL 4.3+) or DSA (GL 4.5+) where available,
 * falls back to legacy glVertexAttribPointer on older GL versions.
 */
public enum VertexAttributeFunctions {
    /**
     * GL 4.5+ Direct State Access path.
     * Operates directly on VAO handles without binding.
     * Uses separate attribute format for flexibility.
     */
    DSA {
        @Override
        public void setupVertexAttribute(int vao, GlVertexAttributeBinding attrib, int bindingIndex) {
            if (attrib.isIntType()) {
                GL45C.glVertexArrayAttribIFormat(vao, attrib.getIndex(), attrib.getCount(),
                        attrib.getFormat(), attrib.getPointer());
            } else {
                GL45C.glVertexArrayAttribFormat(vao, attrib.getIndex(), attrib.getCount(),
                        attrib.getFormat(), attrib.isNormalized(), attrib.getPointer());
            }
            GL45C.glVertexArrayAttribBinding(vao, attrib.getIndex(), bindingIndex);
            GL45C.glEnableVertexArrayAttrib(vao, attrib.getIndex());
        }

        @Override
        public void bindVertexBuffer(int vao, int bindingIndex, GlBuffer buffer, long offset, int stride) {
            GL45C.glVertexArrayVertexBuffer(vao, bindingIndex, buffer.handle(), offset, stride);
        }

        @Override
        public void bindElementBuffer(int vao, GlBuffer buffer) {
            GL45C.glVertexArrayElementBuffer(vao, buffer.handle());
        }

        @Override
        public boolean usesSeparateFormat() {
            return true;
        }

        @Override
        public boolean isDSA() {
            return true;
        }
    },

    /**
     * GL 4.3 separate attribute format path.
     * Uses glVertexAttribFormat + glVertexAttribBinding + glBindVertexBuffer.
     * Requires VAO to be bound.
     */
    SEPARATE_FORMAT {
        @Override
        public void setupVertexAttribute(int vao, GlVertexAttributeBinding attrib, int bindingIndex) {
            // VAO must already be bound
            if (attrib.isIntType()) {
                GL43C.glVertexAttribIFormat(attrib.getIndex(), attrib.getCount(),
                        attrib.getFormat(), attrib.getPointer());
            } else {
                GL43C.glVertexAttribFormat(attrib.getIndex(), attrib.getCount(),
                        attrib.getFormat(), attrib.isNormalized(), attrib.getPointer());
            }
            GL43C.glVertexAttribBinding(attrib.getIndex(), bindingIndex);
            GL30C.glEnableVertexAttribArray(attrib.getIndex());
        }

        @Override
        public void bindVertexBuffer(int vao, int bindingIndex, GlBuffer buffer, long offset, int stride) {
            // VAO must already be bound
            GL43C.glBindVertexBuffer(bindingIndex, buffer.handle(), offset, stride);
        }

        @Override
        public void bindElementBuffer(int vao, GlBuffer buffer) {
            // VAO must already be bound
            GL20C.glBindBuffer(GL20C.GL_ELEMENT_ARRAY_BUFFER, buffer.handle());
        }

        @Override
        public boolean usesSeparateFormat() {
            return true;
        }

        @Override
        public boolean isDSA() {
            return false;
        }
    },

    /**
     * Legacy glVertexAttribPointer path for GL < 4.3.
     * Buffer binding and format are coupled.
     */
    LEGACY {
        @Override
        public void setupVertexAttribute(int vao, GlVertexAttributeBinding attrib, int bindingIndex) {
            // VAO and buffer must already be bound
            if (attrib.isIntType()) {
                GL30C.glVertexAttribIPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat(),
                        attrib.getStride(), attrib.getPointer());
            } else {
                GL20C.glVertexAttribPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat(),
                        attrib.isNormalized(), attrib.getStride(), attrib.getPointer());
            }
            GL20C.glEnableVertexAttribArray(attrib.getIndex());
        }

        @Override
        public void bindVertexBuffer(int vao, int bindingIndex, GlBuffer buffer, long offset, int stride) {
            // In legacy mode, buffer binding is done via glBindBuffer before glVertexAttribPointer
            // This is a no-op as binding is handled in the attribute setup
        }

        @Override
        public void bindElementBuffer(int vao, GlBuffer buffer) {
            // VAO must already be bound
            GL20C.glBindBuffer(GL20C.GL_ELEMENT_ARRAY_BUFFER, buffer.handle());
        }

        @Override
        public boolean usesSeparateFormat() {
            return false;
        }

        @Override
        public boolean isDSA() {
            return false;
        }
    };

    public static VertexAttributeFunctions pickBest(RenderDevice device) {
        GLCapabilities capabilities = device.getCapabilities();

        // Emberbium: both DSA and SEPARATE_FORMAT are new here -- upstream always used the
        // legacy path -- so turning the option off has to fall all the way back to LEGACY,
        // otherwise it is not an escape hatch from a driver bug in either one.
        if (!SodiumClientMod.options().advanced.useModernGlPaths) {
            return LEGACY;
        }

        if (capabilities.OpenGL45) {
            return DSA;
        } else if (capabilities.OpenGL43) {
            return SEPARATE_FORMAT;
        } else {
            return LEGACY;
        }
    }

    /**
     * Sets up a vertex attribute for the given VAO.
     * For LEGACY mode, the buffer must be bound first.
     * For SEPARATE_FORMAT mode, the VAO must be bound.
     * For DSA mode, neither needs to be bound.
     */
    public abstract void setupVertexAttribute(int vao, GlVertexAttributeBinding attrib, int bindingIndex);

    /**
     * Binds a vertex buffer to a binding point.
     * For LEGACY mode, this is a no-op (use glBindBuffer before setupVertexAttribute).
     * For SEPARATE_FORMAT mode, the VAO must be bound.
     * For DSA mode, the VAO does not need to be bound.
     */
    public abstract void bindVertexBuffer(int vao, int bindingIndex, GlBuffer buffer, long offset, int stride);

    /**
     * Binds an element buffer to the VAO.
     */
    public abstract void bindElementBuffer(int vao, GlBuffer buffer);

    /**
     * Returns true if this uses separate format (GL 4.3+ style).
     */
    public abstract boolean usesSeparateFormat();

    /**
     * Returns true if this uses DSA (GL 4.5+ style).
     */
    public abstract boolean isDSA();
}
