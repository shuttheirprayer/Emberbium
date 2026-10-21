package me.jellysquid.mods.sodium.client.gl.tessellation;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.buffer.GlBufferTarget;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.functions.VertexAttributeFunctions;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

public abstract class GlAbstractTessellation implements GlTessellation {
    protected final GlPrimitiveType primitiveType;
    protected final TessellationBinding[] bindings;

    protected GlAbstractTessellation(GlPrimitiveType primitiveType, TessellationBinding[] bindings) {
        this.primitiveType = primitiveType;
        this.bindings = bindings;
    }

    @Override
    public GlPrimitiveType getPrimitiveType() {
        return this.primitiveType;
    }

    protected abstract int getArrayHandle();

    protected void bindAttributes(CommandList commandList) {
        VertexAttributeFunctions vertexFunctions = commandList.getVertexAttributeFunctions();
        int vao = this.getArrayHandle();

        if (vertexFunctions.usesSeparateFormat()) {
            int bindingIndex = 0;
            for (TessellationBinding binding : this.bindings) {
                GlVertexAttributeBinding[] attribs = binding.attributeBindings();

                if (binding.target() == GlBufferTarget.ELEMENT_BUFFER) {
                    vertexFunctions.bindElementBuffer(vao, binding.buffer());
                } else if (attribs.length > 0) {
                    for (GlVertexAttributeBinding attrib : attribs) {
                        vertexFunctions.setupVertexAttribute(vao, attrib, bindingIndex);
                    }

                    int stride = attribs[0].getStride();
                    vertexFunctions.bindVertexBuffer(vao, bindingIndex, binding.buffer(), 0, stride);

                    bindingIndex++;
                }
            }
        } else {
            for (TessellationBinding binding : this.bindings) {
                commandList.bindBuffer(binding.target(), binding.buffer());

                for (GlVertexAttributeBinding attrib : binding.attributeBindings()) {
                    if (attrib.isIntType()) {
                        GL30C.glVertexAttribIPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat(),
                                attrib.getStride(), attrib.getPointer());
                    } else {
                        GL20C.glVertexAttribPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat(), attrib.isNormalized(),
                                attrib.getStride(), attrib.getPointer());
                    }
                    GL20C.glEnableVertexAttribArray(attrib.getIndex());
                }
            }
        }
    }
}
