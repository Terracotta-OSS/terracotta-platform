/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.terracottatech.persistence.sanskrit.file.FileBasedFilesystemDirectory;
import com.terracottatech.utilities.Json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class RepairSanskrit {

  private final Path input;
  private final Path output;
  private final ObjectMapper objectMapper;
  private boolean replace;

  public RepairSanskrit(Path path, ObjectMapper objectMapper) {
    this(path, path.resolve("tmp"), objectMapper);
    this.replace = true;
  }

  public RepairSanskrit(Path input, Path output, ObjectMapper objectMapper) {
    if (!Files.exists(input)) {
      throw new IllegalArgumentException(input.toString());
    }
    if (!Files.exists(output)) {
      try {
        Files.createDirectories(output);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    } else {
      try {
        if (Files.list(output).findAny().isPresent()) {
          throw new IllegalArgumentException(output.toString());
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    this.objectMapper = objectMapper;
    this.input = input;
    this.output = output;
  }

  public Path getInput() {
    return input;
  }

  public Path getOutput() {
    return output;
  }

  public void repairHashes() {
    try (SanskritImpl output = new SanskritImpl(new FileBasedFilesystemDirectory(this.output), objectMapper);
         SanskritImpl input = new SanskritImpl(new FileBasedFilesystemDirectory(this.input), objectMapper) {
           @Override
           String checkHash(String timestamp, String json, String hash) {
             return calculateHash(timestamp, json);
           }

           @Override
           public void onNewRecord(String timestamp, String json) throws SanskritException {
             output.appendRecord(timestamp, json);
           }

           @Override
           String getHashToDelete(HashChecker hashChecker) {
             return null;
           }
         }) {
      requireNonNull(input); // silly statement to get rid of the compilation warning
    } catch (SanskritException e) {
      throw new UncheckedSanskritException(e);
    }
    if (replace) {
      try {
        Files.list(output).forEach(file -> {
          try {
            Files.move(file, input.resolve(file.getFileName()), REPLACE_EXISTING);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
        Files.delete(output);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  public static void main(String[] args) {
    Path root = Paths.get(args[0]);
    RepairSanskrit repairSanskrit = new RepairSanskrit(root.resolve("sanskrit"), Json.copyObjectMapper(true));
    repairSanskrit.repairHashes();
  }

}
