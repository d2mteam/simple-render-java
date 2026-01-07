package com.simplerender.gl.upload;

/**
 * Unit of GPU upload work executed by {@link UploadQueue}.
 */
@FunctionalInterface
public interface UploadTask {
    void execute();
}
