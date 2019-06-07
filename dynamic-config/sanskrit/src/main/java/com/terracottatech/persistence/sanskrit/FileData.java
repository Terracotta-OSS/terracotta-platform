/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

/**
 * An interface representing the ability to read from / write to a file.
 */
public interface FileData extends SeekableByteChannel {
  void force(boolean metaData) throws IOException;
}
