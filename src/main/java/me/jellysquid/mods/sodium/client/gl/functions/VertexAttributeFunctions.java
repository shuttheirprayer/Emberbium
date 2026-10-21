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

public enum VertexAttributeFunctions {
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

    SEPARATE_FORMAT {
        @Override
        public void setupVertexAttribute(int vao, GlVertexAttributeBinding attrib, int bindingIndex) {
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
            GL43C.glBindVertexBuffer(bindingIndex, buffer.handle(), offset, stride);
        }

        @Override
        public void bindElementBuffer(int vao, GlBuffer buffer) {
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

    LEGACY {
        @Override
        public void setupVertexAttribute(int vao, GlVertexAttributeBinding attrib, int bindingIndex) {
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
        }

        @Override
        public void bindElementBuffer(int vao, GlBuffer buffer) {
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

        if (!SodiumClientMod.canUseModernGlPaths()) {
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

    public abstract void setupVertexAttribute(int vao, GlVertexAttributeBinding attrib, int bindingIndex);

    public abstract void bindVertexBuffer(int vao, int bindingIndex, GlBuffer buffer, long offset, int stride);

    public abstract void bindElementBuffer(int vao, GlBuffer buffer);

    public abstract boolean usesSeparateFormat();

    public abstract boolean isDSA();
}
