/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration.util;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtility {
  public static void createDirectory(Path directory) throws Exception {
    if (!Files.exists(directory)) {
      try {
        Files.createDirectory(directory);
      } catch (FileAlreadyExistsException e) {

      }

    }
  }
}
