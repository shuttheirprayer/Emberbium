package me.jellysquid.mods.sodium.client.render.chunk.vertex.format;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;

public interface ChunkVertexType {
    /**
     * @return The scale to be applied to vertex coordinates
     */
    float getPositionScale();

    /**
     * @return The translation to be applied to vertex coordinates
     */
    float getPositionOffset();

    /**
     * @return The scale to be applied to texture coordinates
     */
    float getTextureScale();

    GlVertexFormat<ChunkMeshAttribute> getVertexFormat();

    ChunkVertexEncoder getEncoder();

    default GlVertexAttributeBinding[] getAttributeBindings() {
        GlVertexFormat<ChunkMeshAttribute> format = this.getVertexFormat();

        return new GlVertexAttributeBinding[] {
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID,
                        format.getAttribute(ChunkMeshAttribute.POSITION_MATERIAL_MESH)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_COLOR,
                        format.getAttribute(ChunkMeshAttribute.COLOR_SHADE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE,
                        format.getAttribute(ChunkMeshAttribute.BLOCK_TEXTURE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE,
                        format.getAttribute(ChunkMeshAttribute.LIGHT_TEXTURE))
        };
    }
}
