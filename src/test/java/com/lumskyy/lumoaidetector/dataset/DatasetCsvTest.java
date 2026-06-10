package com.lumskyy.lumoaidetector.dataset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DatasetCsvTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void headerHasExpectedColumnCount() {
        String header = DatasetCsv.header();
        String[] parts = header.split(",", -1);
        assertEquals(DatasetCsv.COLUMN_COUNT, parts.length);
        assertTrue(parts[0].startsWith("dx_1"));
        assertEquals("class", parts[parts.length - 1]);
    }

    @Test
    public void rowHasExpectedColumnCount() {
        double[] features = new double[DatasetCsv.FEATURE_COUNT];
        for (int i = 0; i < features.length; i++) {
            features[i] = i * 0.5D;
        }
        String row = DatasetCsv.row(features, RecordLabel.CHEATER);
        String[] parts = row.split(",", -1);
        assertEquals(DatasetCsv.COLUMN_COUNT, parts.length);
        assertEquals("1", parts[parts.length - 1]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rowRejectsWrongFeatureCount() {
        DatasetCsv.row(new double[3], RecordLabel.LEGIT);
    }

    @Test
    public void countMatchesContentAndSkipsGarbage() throws IOException {
        File file = folder.newFile("dataset.csv");
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            w.write(DatasetCsv.header());
            w.newLine();
            w.write(DatasetCsv.row(zeros(), RecordLabel.LEGIT));
            w.newLine();
            w.write(DatasetCsv.row(zeros(), RecordLabel.CHEATER));
            w.newLine();
            w.write(DatasetCsv.row(zeros(), RecordLabel.CHEATER));
            w.newLine();
            w.write("garbage,row,not,enough,columns");
            w.newLine();
        }
        DatasetCounts counts = DatasetCsv.count(file);
        assertEquals(1, counts.legitRows());
        assertEquals(2, counts.cheaterRows());
        assertEquals(3, counts.rows());
        assertEquals(1, counts.skippedRows());
    }

    @Test
    public void countAndReadAgree() throws IOException {
        File file = folder.newFile("dataset2.csv");
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
            w.write(DatasetCsv.header());
            w.newLine();
            for (int i = 0; i < 5; i++) {
                w.write(DatasetCsv.row(zeros(), RecordLabel.LEGIT));
                w.newLine();
            }
        }
        DatasetCounts counts = DatasetCsv.count(file);
        DatasetSnapshot snapshot = DatasetCsv.read(file);
        assertEquals(snapshot.rows(), counts.rows());
        assertEquals(snapshot.legitRows(), counts.legitRows());
        assertEquals(snapshot.cheaterRows(), counts.cheaterRows());
    }

    @Test
    public void countMissingFileIsZero() throws IOException {
        DatasetCounts counts = DatasetCsv.count(new File(folder.getRoot(), "missing.csv"));
        assertEquals(0, counts.rows());
    }

    private static double[] zeros() {
        return new double[DatasetCsv.FEATURE_COUNT];
    }
}
