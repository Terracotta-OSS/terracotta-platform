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
package org.terracotta.persistence.sanskrit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.persistence.sanskrit.change.SanskritChange;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.terracotta.persistence.sanskrit.MarkableLineParser.LS;

/**
 * The main class for reading and writing a Sanskrit append log.
 * This class is intended to be used by a single thread and so it is not thread-safe.
 */
public class SanskritImpl implements Sanskrit {
  private static final Logger LOGGER = LoggerFactory.getLogger(SanskritImpl.class);

  private static final String APPEND_LOG_FILE = "append.log";
  private static final String HASH_0_FILE = "hash0";
  private static final String HASH_1_FILE = "hash1";
  private static final String FORMAT_VERSION = "format version: ";

  private final FilesystemDirectory filesystemDirectory;
  private final SanskritMapper mapper;

  private volatile MutableSanskritObject data;
  private volatile String lastHash;
  private volatile String nextHashFile;

  public SanskritImpl(FilesystemDirectory filesystemDirectory, SanskritMapper mapper) throws SanskritException {
    this.filesystemDirectory = filesystemDirectory;
    this.mapper = mapper;
    init();
  }

  private void init() throws SanskritException {
    lastHash = null;
    nextHashFile = null;
    this.data = newMutableSanskritObject();

    try {
      List<String> filesToDelete = new ArrayList<>();
      String hash0 = getHashFromFile(HASH_0_FILE, filesToDelete);
      String hash1 = getHashFromFile(HASH_1_FILE, filesToDelete);
      HashChecker hashChecker = new HashChecker(hash0, hash1);

      MutableSanskritObject result = newMutableSanskritObject();

      try (FileData appendLog = filesystemDirectory.getFileData(APPEND_LOG_FILE)) {
        if (appendLog != null) {
          InputStream appendLogStream = new BufferedInputStream(Channels.newInputStream(appendLog));
          MarkableLineParser parser = new MarkableLineParser(appendLogStream);
          Stream<String> lines = parser.lines();
          Stream<Deque<String>> records = groupByEmptyLines(lines);

          AtomicReference<SanskritException> error = new AtomicReference<>();
          AtomicLong counter = new AtomicLong();
          try {
            records.forEach(record -> {
              try {
                if (record.size() < 3) {
                  throw new SanskritException("Invalid record");
                }

                long idx = counter.incrementAndGet();

                String timestamp;
                String version;
                String first = record.removeFirst();
                if (first.startsWith(FORMAT_VERSION)) {
                  // V2 and so on
                  timestamp = record.removeFirst();
                  version = first.substring(16);
                } else {
                  // V1 change format don't have a version flag
                  timestamp = first;
                  version = "";
                }
                String hash = record.removeLast();
                String data = String.join(LS, record);

                LOGGER.trace("init(): record {}: timestamp={}, version={}, hash={}, data={}", idx, timestamp, version, hash, data);

                hash = checkHash(timestamp, data, hash);
                String hashedHash = HashUtils.generateHash(hash);
                boolean acceptRecord = hashChecker.check(hashedHash);

                LOGGER.trace("init(): record {}: hash={}, hashedHash={}, acceptRecord={}", idx, hash, hashedHash, acceptRecord);

                if (acceptRecord) {
                  parser.mark();
                  mapper.fromString(data, version, result);
                  onNewRecord(timestamp, data);
                  lastHash = hash;
                }
              } catch (SanskritException e) {
                error.set(e);
                throw new UncheckedSanskritException(e);
              }
            });
          } catch (UncheckedSanskritException e) {
            if (error.get() != null) {
              throw error.get();
            } else {
              throw e;
            }
          }

          long mark = parser.getMark();
          if (mark == 0) {
            filesToDelete.add("append.log");
          } else {
            try {
              appendLog.truncate(mark);
            } catch (IOException e) {
              throw new SanskritException(e);
            }
          }
        }
      }

      String hashToDelete = getHashToDelete(hashChecker);
      if (hashToDelete != null) {
        filesToDelete.add(hashToDelete);
      }

      LOGGER.trace("init(): filesToDelete={}", filesToDelete);

      for (String file : filesToDelete) {
        filesystemDirectory.delete(file);
      }

      nextHashFile = hashChecker.nextHashFile();

      this.data = result;
    } catch (IOException e) {
      throw new SanskritException(e);
    }
  }

  String getHashToDelete(HashChecker hashChecker) throws SanskritException {
    return hashChecker.done();
  }

  protected void onNewRecord(String timestamp, String data) throws SanskritException {
  }

  private String getHashFromFile(String hashFile, List<String> filesToDelete) throws SanskritException {
    LOGGER.trace("getHashFromFile({}, {})", hashFile, filesToDelete);

    ByteBuffer hashBuffer = ByteBuffer.allocate(40);

    try (FileData fileData = filesystemDirectory.getFileData(hashFile)) {
      if (fileData == null) {
        LOGGER.trace("getHashFromFile({}): <none>", hashFile);
        return null;
      }

      if (fileData.size() > 40) {
        throw new SanskritException("Hash file too long: " + hashFile);
      }

      if (fileData.size() < 40) {
        filesToDelete.add(hashFile);
        LOGGER.trace("getHashFromFile({}): <none>, {}", hashFile, filesToDelete);
        return null;
      }

      while (hashBuffer.hasRemaining()) {
        int read = fileData.read(hashBuffer);
        if (read == -1) {
          break;
        }
      }
    } catch (IOException e) {
      throw new SanskritException(e);
    }

    hashBuffer.flip();
    String hash = StandardCharsets.UTF_8.decode(hashBuffer).toString();
    LOGGER.trace("getHashFromFile({}): {}", hashFile, hash);
    return hash;
  }

  String checkHash(String timestamp, String data, String hash) throws SanskritException {
    String expectedHash = calculateHash(timestamp, data);
    if (!hash.equals(expectedHash)) {
      throw new SanskritException("Hash mismatch. Got: " + hash + ". Computed: " + expectedHash);
    }
    return hash;
  }

  String calculateHash(String timestamp, String data) {
    LOGGER.trace("calculateHash({}, {})", timestamp, data);
    if (lastHash == null) {
      return HashUtils.generateHash(
          timestamp,
          LS,
          data
      );
    } else {
      return HashUtils.generateHash(
          lastHash,
          LS,
          LS,
          timestamp,
          LS,
          data
      );
    }
  }

  private Stream<Deque<String>> groupByEmptyLines(Stream<String> lines) {
    return StreamSupport.stream(new GroupingSpliterator(lines), false);
  }

  @Override
  public void close() {
  }

  @Override
  public String getString(String key) throws SanskritException {
    return data.getString(key);
  }

  @Override
  public Long getLong(String key) throws SanskritException {
    return data.getLong(key);
  }

  @Override
  public SanskritObject getObject(String key) throws SanskritException {
    final SanskritObject found = data.getObject(key);
    if (found == null) {
      return null;
    }
    SanskritObjectImpl copy = new SanskritObjectImpl(mapper);
    found.accept(copy);
    return copy;
  }

  @Override
  public void applyChange(SanskritChange change) throws SanskritException {
    change.accept(data);
    appendChange(change);
  }

  @Override
  public MutableSanskritObject newMutableSanskritObject() {
    return new SanskritObjectImpl(mapper);
  }

  @Override
  public void reset() throws SanskritException {
    try {
      filesystemDirectory.delete(HASH_0_FILE);
      filesystemDirectory.delete(HASH_1_FILE);
      filesystemDirectory.backup(APPEND_LOG_FILE);
      init();
    } catch (IOException e) {
      throw new SanskritException(e);
    }
  }

  private void appendChange(SanskritChange change) throws SanskritException {
    String data = mapper.toString(change);
    LOGGER.trace("appendChange(): {}", data);
    appendChange(data);
  }

  private void appendChange(String data) throws SanskritException {
    String timestamp = getTimestamp();
    appendRecord(timestamp, data);
  }

  void appendRecord(String timestamp, String data) throws SanskritException {
    LOGGER.trace("appendRecord({}, {})", timestamp, data);
    String hash = calculateHash(timestamp, data);
    appendEntry((FORMAT_VERSION + mapper.getCurrentFormatVersion()) + LS + timestamp + LS + data + LS + hash + LS + LS, hash);
  }

  private String getTimestamp() {
    return Instant.now().toString();
  }

  private void appendEntry(String logEntry, String entryHash) throws SanskritException {
    LOGGER.trace("appendEntry({}, {})", logEntry, entryHash);
    String finalHash = HashUtils.generateHash(entryHash);
    LOGGER.trace("appendEntry({}): finalHash: {}", entryHash, finalHash);

    try (
        FileData appendLog = getAppendLogForAppend();
        FileData hashFile = createNewHashFile()
    ) {
      write(appendLog, logEntry);
      write(hashFile, finalHash);

      nextHashFile = flipHashFile();
      filesystemDirectory.delete(nextHashFile);

      lastHash = entryHash;
    } catch (IOException e) {
      throw new SanskritException(e);
    }
  }

  private String flipHashFile() {
    if (Objects.equals(nextHashFile, HASH_0_FILE)) {
      return HASH_1_FILE;
    }

    return HASH_0_FILE;
  }

  private FileData getAppendLogForAppend() throws SanskritException {
    try (Owner<FileData, IOException> appendLogOwner = Owner.own(filesystemDirectory.create(APPEND_LOG_FILE, true), IOException.class)) {
      FileData appendLog = appendLogOwner.borrow();
      appendLog.position(appendLog.size());
      return appendLogOwner.release();
    } catch (IOException e) {
      throw new SanskritException(e);
    }
  }

  private FileData createNewHashFile() throws SanskritException {
    try {
      return filesystemDirectory.create(nextHashFile, false);
    } catch (IOException e) {
      throw new SanskritException(e);
    }
  }

  private void write(FileData fileData, String text) throws SanskritException {
    try {
      ByteBuffer bytes = StandardCharsets.UTF_8.encode(text);

      while (bytes.hasRemaining()) {
        fileData.write(bytes);
      }

      fileData.force(false);
    } catch (IOException e) {
      throw new SanskritException(e);
    }
  }
}
