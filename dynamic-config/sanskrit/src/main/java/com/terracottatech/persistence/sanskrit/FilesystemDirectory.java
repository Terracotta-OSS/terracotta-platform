/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import java.io.IOException;

/**
 * An interface representing files in a directory.
 */
public interface FilesystemDirectory {
  DirectoryLock lock() throws IOException;

  FileData create(String filename, boolean canExist) throws IOException;

  FileData getFileData(String filename) throws IOException;

  void delete(String filename) throws IOException;
}
