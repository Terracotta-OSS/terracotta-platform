/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.xml.plugins;

import com.terracottatech.data.config.DataRootMapping;
import com.terracottatech.utilities.PathResolver;
import com.terracottatech.utilities.junit.TmpDir;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DataDirectoriesElementTest {

  @Rule
  public TmpDir temporaryFolder = new TmpDir();

  PathResolver pathResolver;

  @Before
  public void setUp() {
    pathResolver = new PathResolver(temporaryFolder.getRoot());
  }

  @Test
  public void testCreateDataDirectories() {
    Path dataRoot1 = temporaryFolder.getRoot().resolve("user-data-1");
    Path dataRoot2 = temporaryFolder.getRoot().resolve("user-data-2");
    Path metadataRoot = temporaryFolder.getRoot().resolve("metadata");

    Map<String, Path> dataRootMap = new HashMap<>();
    dataRootMap.put("data-root-1", dataRoot1);
    dataRootMap.put("data-root-2", dataRoot2);

    com.terracottatech.data.config.DataDirectories dataDirectories =
        new DataDirectories(dataRootMap, metadataRoot, pathResolver).createDataDirectories();

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
  public void testCreateDataDirectoriesWithOverlappingMetadataRoot() {
    Path dataRoot1 = temporaryFolder.getRoot().resolve("user-data-1");
    Path dataRoot2 = temporaryFolder.getRoot().resolve("user-data-2");

    Map<String, Path> dataRootMap = new HashMap<>();
    dataRootMap.put("data-root-1", dataRoot1);
    dataRootMap.put("data-root-2", dataRoot2);

    com.terracottatech.data.config.DataDirectories dataDirectories =
        new DataDirectories(dataRootMap, dataRoot1, pathResolver).createDataDirectories();

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

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("Pair{");
      sb.append("path='").append(path).append('\'');
      sb.append(", isPlatformRoot=").append(isPlatformRoot);
      sb.append('}');
      return sb.toString();
    }
  }

}