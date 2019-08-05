/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.persistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FileConfigStorage implements ConfigStorage<String> {
  private final Path root;
  private final Function<Long, String> filenameGenerator;

  public FileConfigStorage(Path root, Function<Long, String> filenameGenerator) {
    this.root = root;
    this.filenameGenerator = filenameGenerator;
  }

  @Override
  public String getConfig(long version) throws ConfigStorageException {
    Path file = toPath(version);

    try {
      byte[] bytes = Files.readAllBytes(file);
      return new String(bytes, UTF_8);
    } catch (IOException e) {
      throw new ConfigStorageException(e);
    }
  }

  @Override
  public void saveConfig(long version, String config) throws ConfigStorageException {
    File file = toFile(version);

    try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
      writer.print(config);
    } catch (FileNotFoundException | UnsupportedEncodingException e) {
      throw new ConfigStorageException(e);
    }
  }

  private File toFile(long version) {
    Path filePath = toPath(version);
    return filePath.toFile();
  }

  private Path toPath(long version) {
    String filename = filenameGenerator.apply(version);
    return root.resolve(filename);
  }
}
