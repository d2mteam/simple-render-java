package com.simplerender.gl.upload;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class UploadQueue {
    private final Queue<UploadTask> tasks = new ConcurrentLinkedQueue<>();
    private final StaticMeshUploader staticMeshUploader;
    private final TextureStreamingUploader textureStreamingUploader;
    private final SparseTextureUploader sparseTextureUploader;
    private final DynamicVboUploader dynamicVboUploader;
    private final EcsRenderDataDoubleBuffer ecsRenderDataDoubleBuffer;

    public UploadQueue(
        StaticMeshUploader staticMeshUploader,
        TextureStreamingUploader textureStreamingUploader,
        SparseTextureUploader sparseTextureUploader,
        DynamicVboUploader dynamicVboUploader,
        EcsRenderDataDoubleBuffer ecsRenderDataDoubleBuffer
    ) {
        this.staticMeshUploader = staticMeshUploader;
        this.textureStreamingUploader = textureStreamingUploader;
        this.sparseTextureUploader = sparseTextureUploader;
        this.dynamicVboUploader = dynamicVboUploader;
        this.ecsRenderDataDoubleBuffer = ecsRenderDataDoubleBuffer;
    }

    public void enqueue(UploadTask task) {
        tasks.add(task);
    }

    public void process() {
        UploadTask task;
        while ((task = tasks.poll()) != null) {
            task.execute();
        }
    }

    public StaticMeshUploader staticMeshUploader() {
        return staticMeshUploader;
    }

    public TextureStreamingUploader textureStreamingUploader() {
        return textureStreamingUploader;
    }

    public SparseTextureUploader sparseTextureUploader() {
        return sparseTextureUploader;
    }

    public DynamicVboUploader dynamicVboUploader() {
        return dynamicVboUploader;
    }

    public EcsRenderDataDoubleBuffer ecsRenderDataDoubleBuffer() {
        return ecsRenderDataDoubleBuffer;
    }
}
