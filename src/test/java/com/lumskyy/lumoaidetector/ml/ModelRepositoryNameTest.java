package com.lumskyy.lumoaidetector.ml;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ModelRepositoryNameTest {

    @Test
    public void rejectsPathTraversal() {
        assertNull(ModelRepository.cleanName("../evil"));
        assertNull(ModelRepository.cleanName("..\\evil"));
        assertNull(ModelRepository.cleanName("sub/dir"));
        assertNull(ModelRepository.cleanName("sub\\dir"));
    }

    @Test
    public void rejectsIllegalCharacters() {
        assertNull(ModelRepository.cleanName("name with space"));
        assertNull(ModelRepository.cleanName("name*star"));
        assertNull(ModelRepository.cleanName(null));
    }

    @Test
    public void stripsExtensions() {
        assertEquals("model-1", ModelRepository.cleanName("model-1.bin"));
        assertEquals("model-1", ModelRepository.cleanName("model-1.yml"));
    }

    @Test
    public void keepsValidNames() {
        assertEquals("model-2026-01-01_12-00-00", ModelRepository.cleanName("model-2026-01-01_12-00-00"));
        assertEquals("My.Model_3", ModelRepository.cleanName("My.Model_3"));
    }
}
