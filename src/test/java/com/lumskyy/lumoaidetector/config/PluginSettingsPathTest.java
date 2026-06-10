package com.lumskyy.lumoaidetector.config;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PluginSettingsPathTest {

    @Test
    public void rejectsTraversalAndAbsolutePaths() {
        assertEquals("data/dataset.csv", PluginSettings.cleanRelativePath("../escape.csv"));
        assertEquals("data/dataset.csv", PluginSettings.cleanRelativePath("/etc/passwd"));
        assertEquals("data/dataset.csv", PluginSettings.cleanRelativePath("\\windows\\system"));
        assertEquals("data/dataset.csv", PluginSettings.cleanRelativePath("C:\\secret.csv"));
    }

    @Test
    public void blankFallsBackToDefault() {
        assertEquals("data/dataset.csv", PluginSettings.cleanRelativePath(""));
        assertEquals("data/dataset.csv", PluginSettings.cleanRelativePath("   "));
        assertEquals("data/dataset.csv", PluginSettings.cleanRelativePath(null));
    }

    @Test
    public void normalizesBackslashesInRelativePath() {
        assertEquals("data/sub/file.csv", PluginSettings.cleanRelativePath("data\\sub\\file.csv"));
    }

    @Test
    public void keepsCleanRelativePath() {
        assertEquals("data/dataset.csv", PluginSettings.cleanRelativePath("data/dataset.csv"));
    }
}
