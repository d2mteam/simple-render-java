package com.simplerender.render.gl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.simplerender.asset.SamplerData;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

class GpuSamplerTest {
    @Test
    void mapsWrapModes() {
        assertEquals(GL12.GL_CLAMP_TO_EDGE, GpuSampler.toGlWrap(SamplerData.CLAMP_TO_EDGE));
        assertEquals(GL14.GL_MIRRORED_REPEAT, GpuSampler.toGlWrap(SamplerData.MIRRORED_REPEAT));
        assertEquals(GL11.GL_REPEAT, GpuSampler.toGlWrap(SamplerData.REPEAT));
    }

    @Test
    void mapsFiltersAndFallsBackToLinear() {
        assertEquals(GL11.GL_LINEAR_MIPMAP_LINEAR, GpuSampler.toGlMinFilter(SamplerData.LINEAR_MIPMAP_LINEAR));
        assertEquals(GL11.GL_NEAREST, GpuSampler.toGlMinFilter(SamplerData.NEAREST));
        assertEquals(GL11.GL_LINEAR, GpuSampler.toGlMinFilter(12345));
        // Magnification can't use mipmaps: anything that isn't NEAREST becomes LINEAR.
        assertEquals(GL11.GL_LINEAR, GpuSampler.toGlMagFilter(SamplerData.LINEAR_MIPMAP_LINEAR));
        assertEquals(GL11.GL_NEAREST, GpuSampler.toGlMagFilter(SamplerData.NEAREST));
    }
}
