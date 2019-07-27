/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml.plugins;

import com.terracottatech.data.config.DataRootMapping;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DataDirectoriesElementTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  Supplier<Path> basedir = () -> Paths.get("");

  @Test
  public void testCreateDataDirectories() throws IOException {
    Path dataRoot1 = temporaryFolder.newFolder().toPath();
    Path dataRoot2 = temporaryFolder.newFolder().toPath();
    Path metadataRoot = temporaryFolder.newFolder().toPath();

    Map<String, Path> dataRootMap = new HashMap<>();
    dataRootMap.put("data-root-1", dataRoot1);
    dataRootMap.put("data-root-2", dataRoot2);

    com.terracottatech.data.config.DataDirectories dataDirectories =
        new DataDirectories(dataRootMap, metadataRoot, basedir).createDataDirectories();

    Map<String, Pair> expected = new HashMap<>();
    expected.put("data-root-1", new Pair(dataRoot1.toString(), false));
    expected.put("data-root-2", new Pair(dataRoot2.toString(), false));
    expected.put(DataDirectories.META_DATA_ROOT_NAME, new Pair(metadataRoot.toString(), true));

    List<DataRootMapping> dataRootMappings = dataDirectories.getDirectory();
    Map<String, Pair> actual = new HashMap<>();

    for (DataRootMapping dataRootMapping : dataRootMappings) {
      actual.put(dataRootMapping.getName(), new Pair(dataRootMapping.getValue(), dataRootMapping.isUseForPlatform()));
    }

    assertThat(actual, is(expected));
  }

  @Test
  public void testCreateDataDirectoriesWithOverlappingMetadataRoot() throws IOException {
    Path dataRoot1 = temporaryFolder.newFolder().toPath();
    Path dataRoot2 = temporaryFolder.newFolder().toPath();

    Map<String, Path> dataRootMap = new HashMap<>();
    dataRootMap.put("data-root-1", dataRoot1);
    dataRootMap.put("data-root-2", dataRoot2);

    com.terracottatech.data.config.DataDirectories dataDirectories =
        new DataDirectories(dataRootMap, dataRoot1, basedir).createDataDirectories();

    Map<String, Pair> expected = new HashMap<>();
    expected.put("data-root-1", new Pair(dataRoot1.toString(), true));
    expected.put("data-root-2", new Pair(dataRoot2.toString(), false));

    List<DataRootMapping> dataRootMappings = dataDirectories.getDirectory();
    Map<String, Pair> actual = new HashMap<>();

    for (DataRootMapping dataRootMapping : dataRootMappings) {
      actual.put(dataRootMapping.getName(), new Pair(dataRootMapping.getValue(), dataRootMapping.isUseForPlatform()));
    }

    assertThat(actual, is(expected));
  }

  private static class Pair {
    private final String path;
    private final boolean isPlatformRoot;

    private Pair(String path, boolean isPlatformRoot) {
      this.path = path;
      this.isPlatformRoot = isPlatformRoot;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final Pair pair = (Pair) o;
      return isPlatformRoot == pair.isPlatformRoot &&
          Objects.equals(path, pair.path);
    }

    @Override
    public int hashCode() {
      return Objects.hash(path, isPlatformRoot);
    }
  }

}