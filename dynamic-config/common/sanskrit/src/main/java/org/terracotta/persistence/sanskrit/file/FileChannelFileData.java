/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
