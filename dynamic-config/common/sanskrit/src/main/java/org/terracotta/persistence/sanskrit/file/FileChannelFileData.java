/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit.file;

import org.terracotta.persistence.sanskrit.FileData;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * A simple adapter of FileChannel to meet the FileData interface.
 */
public class FileChannelFileData implements FileData {
  private final FileChannel channel;

  public FileChannelFileData(FileChannel channel) {
    this.channel = channel;
  }

  @Override
  public long size() throws IOException {
    return channel.size();
  }

  @Override
  public long position() throws IOException {
    return channel.position();
  }

  @Override
  public FileData position(long newPosition) throws IOException {
    channel.position(newPosition);
    return this;
  }

  @Override
  public int write(ByteBuffer src) throws IOException {
    return channel.write(src);
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    return channel.read(dst);
  }

  @Override
  public FileData truncate(long size) throws IOException {
    channel.truncate(size);
    return this;
  }

  @Override
  public void force(boolean metaData) throws IOException {
    channel.force(metaData);
  }

  @Override
  public boolean isOpen() {
    return channel.isOpen();
  }

  @Override
  public void close() throws IOException {
    channel.close();
  }
}
