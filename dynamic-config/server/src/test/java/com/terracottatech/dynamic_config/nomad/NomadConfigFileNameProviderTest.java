/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class NomadConfigFileNameProviderTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test(expected = NomadConfigFileNameProviderException.class)
  public void testGetNodeNameWithEmptyList() {
    NomadConfigFileNameProvider.getNodeName(new ArrayList<>());
  }

  @Test(expected = NomadConfigFileNameProviderException.class)
  public void testGetNodeNameWithNullList() {
    NomadConfigFileNameProvider.getNodeName(new ArrayList<>());
  }

  @Test(expected = NomadConfigFileNameProviderException.class)
  public void testGetNodeNameWithInvalidFormat() {
    List<String> input = new ArrayList<>();
    input.add("cluster-config.1.xml");
    NomadConfigFileNameProvider.getNodeName(input);
  }

  @Test(expected = NomadConfigFileNameProviderException.class)
  public void testGetNodeNameWithInvalidInitialPart() {
    List<String> input = new ArrayList<>();
    input.add("xxx.node0.1.xml");
    NomadConfigFileNameProvider.getNodeName(input);
  }

  @Test(expected = NomadConfigFileNameProviderException.class)
  public void testGetNodeNameWithDifferentFileNames() {
    List<String> input = new ArrayList<>();
    input.add("cluster-config.node0.1.xml");
    input.add("cluster-config.node1.1.xml");
    NomadConfigFileNameProvider.getNodeName(input);
  }

  @Test
  public void testGetNodeName() {
    List<String> input = new ArrayList<>();
    input.add("cluster-config.node0.1.xml");
    String nodeName = NomadConfigFileNameProvider.getNodeName(input);
    assertEquals("node0", nodeName);
  }

  @Test
  public void testGetNodeNameMultipleEntries() {
    List<String> input = new ArrayList<>();
    input.add("cluster-config.node0.1.xml");
    input.add("cluster-config.node0.2.xml");
    String nodeName = NomadConfigFileNameProvider.getNodeName(input);
    assertEquals("node0", nodeName);
  }

  @Test(expected = NomadConfigFileNameProviderException.class)
  public void testGetNodeNameWithInvalidFormatMultiEntry() {
    List<String> input = new ArrayList<>();
    input.add("cluster-config.node0.1.xml");
    input.add("cluster-config.1.xml");
    NomadConfigFileNameProvider.getNodeName(input);
  }

  @Test(expected = NomadConfigFileNameProviderException.class)
  public void testGetNodeNameWithInvalidInitialPartMultiEntry() {
    List<String> input = new ArrayList<>();
    input.add("cluster-config.node0.1.xml");
    input.add("xxx.node0.1.xml");
    NomadConfigFileNameProvider.getNodeName(input);
  }

  @Test(expected = NomadConfigFileNameProviderException.class)
  public void testGetFileNameProviderNullNodeName() {
    ConfigController configController = new ConfigController() {
      @Override
      public String getStripeName() {
        return null;
      }

      @Override
      public String getNodeName() {
        return null;
      }

      @Override
      public Measure<MemoryUnit> getOffheapSize(final String name) throws ConfigControllerException {
        return Measure.zero(MemoryUnit.class);
      }

      @Override
      public void setOffheapSize(String name, Measure<MemoryUnit> newOffheapSize) throws ConfigControllerException {

      }
    };
    Path path = mock(Path.class);
    Function<Long, String> fileNameGeneratorFunction = NomadConfigFileNameProvider.getFileNameProvider(path, configController);
    fileNameGeneratorFunction.apply(1L);
  }

  @Test
  public void testGetFileNameProviderWithNameFromController() {
    ConfigController configController = new ConfigController() {
      @Override
      public String getStripeName() {
        return null;
      }

      @Override
      public String getNodeName() {
        return "node0";
      }

      @Override
      public Measure<MemoryUnit> getOffheapSize(final String name) throws ConfigControllerException {
        return Measure.zero(MemoryUnit.class);
      }

      @Override
      public void setOffheapSize(final String name, Measure<MemoryUnit> newOffheapSize) throws ConfigControllerException {

      }
    };
    Path path = mock(Path.class);
    Function<Long, String> fileNameGeneratorFunction = NomadConfigFileNameProvider.getFileNameProvider(path, configController);
    String nodeName = fileNameGeneratorFunction.apply(1L);
    assertEquals("cluster-config.node0.1.xml", nodeName);
  }

  @Test
  public void testGetFileNameProvider() {
    createFile("cluster-config.node0.1.xml", "Hello World!");
    Path path = temporaryFolder.getRoot().toPath();
    Function<Long, String> fileNameGeneratorFunction = NomadConfigFileNameProvider.getFileNameProvider(path, null);
    String nodeName = fileNameGeneratorFunction.apply(1L);
    assertEquals("cluster-config.node0.1.xml", nodeName);
  }

  @Test(expected = NomadConfigFileNameProviderException.class)
  public void testGetFileNameProviderWithEmptyFolder() {
    Path path = temporaryFolder.getRoot().toPath();
    Function<Long, String> fileNameGeneratorFunction = NomadConfigFileNameProvider.getFileNameProvider(path, null);
    fileNameGeneratorFunction.apply(1L);
  }

  private void createFile(String fileName, String content) {
    File file = toFile(fileName);

    try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
      writer.print(content);
    } catch (FileNotFoundException | UnsupportedEncodingException e) {
      throw new UncheckedIOException(e);
    }
  }

  private File toFile(String fileName) {
    Path filePath = toPath(fileName);
    return filePath.toFile();
  }

  private Path toPath(String fileName) {
    Path root = temporaryFolder.getRoot().toPath();
    return root.resolve(fileName);
  }
}