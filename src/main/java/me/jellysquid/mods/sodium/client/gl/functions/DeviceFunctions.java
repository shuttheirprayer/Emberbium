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

        // Log the selection: which paths are live depends on the driver's reported version and
        // on the Direct State Access option, and without this there is no way to tell from a log
        // whether a run actually used DSA.
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
