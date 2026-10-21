package me.jellysquid.mods.sodium.client.gl.functions;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;

public class DeviceFunctions {
    private final BufferStorageFunctions bufferStorageFunctions;
    private final BufferDataFunctions bufferDataFunctions;
    private final VertexAttributeFunctions vertexAttributeFunctions;

    public DeviceFunctions(RenderDevice device) {
        this.bufferStorageFunctions = BufferStorageFunctions.pickBest(device);
        this.bufferDataFunctions = BufferDataFunctions.pickBest(device);
        this.vertexAttributeFunctions = VertexAttributeFunctions.pickBest(device);

        SodiumClientMod.logger().info("Selected GL function paths: storage={}, data={}, vertexAttribute={}",
                this.bufferStorageFunctions, this.bufferDataFunctions, this.vertexAttributeFunctions);
    }

    public BufferStorageFunctions getBufferStorageFunctions() {
        return this.bufferStorageFunctions;
    }

    public BufferDataFunctions getBufferDataFunctions() {
        return this.bufferDataFunctions;
    }

    public VertexAttributeFunctions getVertexAttributeFunctions() {
        return this.vertexAttributeFunctions;
    }
}
