package com.deevvi.async.publisher.utils;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test for {@link FileUtils} class.
 */
public class FileUtilsTest {

    @Test
    public void testGenerateLogFileName() {

        //setup
        Pattern pattern = Pattern.compile("metrics-logs-\\d{4}-\\d{1,2}-\\d{1,2}-\\d{1,2}.log");

        //call
        String fileName = FileUtils.generateLogFileTimeRollingSuffix();

        //verify
        Assertions.assertNotNull(fileName);
        Assertions.assertTrue(fileName.length() > 0);
        Assertions.assertTrue(pattern.matcher(fileName).find());
    }

    @Test
    public void testGeneratePropertiesFileEmptyFile() {

        Assertions.assertThrows(NullPointerException.class, () -> FileUtils.generatePropertiesFileNameForLogFile("   "));
    }

    @Test
    public void testGeneratePropertiesFile() {

        //setup
        String logFile = "metrics-logs-2019-07-30-07.log";

        //call
        String propertiesFile = FileUtils.generatePropertiesFileNameForLogFile(logFile);

        //verify
        assertThat(propertiesFile).isNotNull();
        assertThat(propertiesFile).isNotEmpty();
        assertThat(propertiesFile).isEqualTo("metrics-logs-2019-07-30-07.properties");
    }

    @Test
    public void testIsLogFile() {

        //verify
        assertThat(FileUtils.isLogFile(new File("metrics-logs-2019-07-30-07.log"))).isTrue();
        assertThat(FileUtils.isLogFile(new File("metrics-logs-2011-10-30-07.log"))).isTrue();

        assertThat(FileUtils.isLogFile(new File("metrics-logs-2011-10-30-07.properties"))).isFalse();
        assertThat(FileUtils.isLogFile(new File("metrics.logs-2011-10-30-07.log"))).isFalse();
        assertThat(FileUtils.isLogFile(new File("metrics-logs-2011-10-30-07.logg"))).isFalse();
        assertThat(FileUtils.isLogFile(new File("metrics_logs-2011-10-30-07"))).isFalse();
        assertThat(FileUtils.isLogFile(new File(" metrics-logs-2011-10-30-07.log "))).isFalse();
    }

    @Test
    public void testPathWithoutEndingSlash(@TempDir Path tempDir) throws IOException {

        //setup
        File file = Files.createDirectory(Paths.get(tempDir.toString()+"/logs")).toFile();

        //call
        String result = FileUtils.validatePath(file.getPath());

        //verify
        assertThat(result).endsWith("/");
    }

    @Test
    public void testFolderIsCreated(@TempDir Path tempDir) {

        //setup
        File file = tempDir.toFile();

        //call
        String result = FileUtils.validatePath(file.getAbsolutePath() + "/my-logs-path");

        //verify
        assertThat(result).endsWith("/");
        assertThat(new File(result).exists()).isTrue();
        assertThat(new File(result).isDirectory()).isTrue();
    }

    @Test
    public void testThrowExceptionIfPointsToFile(@TempDir Path tempDir) throws IOException {

        //setup
        File file = new File(tempDir.toFile().getAbsolutePath()+"/myfile.txt/");
        file.createNewFile();

        //call
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtils.validatePath(file.getAbsolutePath()));
    }
}
