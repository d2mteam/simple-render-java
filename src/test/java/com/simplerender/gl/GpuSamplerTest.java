package com.simplerender.gl;

import com.simplerender.asset.SamplerData;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GpuSamplerTest {
    @Test
    void normalizeWrapMapsClampToEdge() {
        assertEquals(GL12.GL_CLAMP_TO_EDGE, GpuSampler.normalizeWrap(SamplerData.CLAMP_TO_EDGE));
    }

    @Test
    void normalizeWrapKeepsRepeatModes() {
        assertEquals(GL11.GL_REPEAT, GpuSampler.normalizeWrap(SamplerData.REPEAT));
        assertEquals(GL14.GL_MIRRORED_REPEAT, GpuSampler.normalizeWrap(SamplerData.MIRRORED_REPEAT));
    }
}
