package com.simplerender.gl.upload;

/**
 * Factory for creating persistently mapped dynamic vertex buffers.
 */
public final class DynamicVboUploader {
    public PersistentMappedVbo create() {
        return new PersistentMappedVbo();
    }
}
