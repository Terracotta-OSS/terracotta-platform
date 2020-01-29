/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.persistence.sanskrit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

public class MemoryFilesystemDirectory implements FilesystemDirectory {
  private static final Charset UTF_8 = Charset.forName("UTF-8");

  private final ConcurrentMap<String, String> files = new ConcurrentHashMap<>();
  private final Set<String> locks = new HashSet<>();
  private final Semaphore directoryLock = new Semaphore(1);
  private boolean fail;

  public void fail() {
    fail = true;
  }

  @Override
  public DirectoryLock lock() throws IOException {
    checkFail();

    if (directoryLock.tryAcquire()) {
      return directoryLock::release;
    } else {
      throw new IOException("Failed to acquire directory lock");
    }
  }

  @Override
  public FileData create(String filename, boolean canExist) throws IOException {
    checkFail();

    if (files.containsKey(filename)) {
      if (!canExist) {
        throw new IOException("File exists: " + filename);
      }

      if (locks.contains(filename)) {
        throw new IOException("File open: " + filename);
      }
    } else {
      files.put(filename, "");
    }

    locks.add(filename);

    return new MemoryFileData(filename);
  }

  @Override
  public FileData getFileData(String filename) throws IOException {
    checkFail();

    if (!files.containsKey(filename)) {
      return null;
    }

    if (locks.contains(filename)) {
      throw new IOException("File open: " + filename);
    }

    locks.add(filename);

    return new MemoryFileData(filename);
  }

  @Override
  public void delete(String filename) throws IOException {
    checkFail();
    files.remove(filename);
  }

  private void checkFail() throws IOException {
    if (fail) {
      throw new IOException("fail");
    }
  }

  private class MemoryFileData implements FileData {
    private final String filename;
    private final AtomicInteger position;
    private volatile boolean closed;

    private MemoryFileData(String filename) {
      this.filename = filename;
      this.position = new AtomicInteger(0);
      this.closed = false;
    }

    @Override
    public void force(boolean metaData) throws IOException {
      checkFail();
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
      checkFail();

      int writeLength = src.remaining();
      int writePosition = position.get();
      int writeExtent = writePosition + writeLength;

      files.compute(filename, (k, v) -> {
        byte[] bytes = v.getBytes(UTF_8);
        int newLength = Math.max(bytes.length, writeExtent);

        ByteBuffer newValue = ByteBuffer.allocate(newLength);
        newValue.put(bytes);
        newValue.position(writePosition);
        newValue.put(src);
        newValue.flip();

        return UTF_8.decode(newValue).toString();
      });

      position.set(writeExtent);

      return writeLength;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
      checkFail();

      String data = files.get(filename);
      byte[] bytes = data.getBytes(UTF_8);

      int readPosition = position.get();
      if (readPosition >= bytes.length) {
        return -1;
      }

      int readLength = Math.min(dst.remaining(), bytes.length - readPosition);
      dst.put(bytes, readPosition, readLength);
      readPosition += readLength;
      position.set(readPosition);

      return readLength;
    }

    @Override
    public long position() throws IOException {
      checkFail();
      return position.get();
    }

    @Override
    public FileData position(long newPosition) throws IOException {
      checkFail();
      int newIntPosition = (int) newPosition;
      this.position.set(newIntPosition);
      return this;
    }

    @Override
    public long size() throws IOException {
      checkFail();
      return files.get(filename).length();
    }

    @Override
    public FileData truncate(long size) throws IOException {
      checkFail();

      int intSize = (int) size;

      files.compute(filename, (k, v) -> {
        if (v.length() > size) {
          return v.substring(0, intSize);
        } else {
          return v;
        }
      });

      position.getAndUpdate(p -> Math.min(p, intSize));

      return this;
    }

    @Override
    public boolean isOpen() {
      return !closed;
    }

    @Override
    public void close() {
      closed = true;
      assertTrue(locks.remove(filename));
    }
  }
}
