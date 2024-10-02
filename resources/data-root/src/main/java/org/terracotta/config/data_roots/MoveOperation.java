/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2025
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
package org.terracotta.config.data_roots;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class MoveOperation {

  private static final String MARK_FILE = "mark.txt";
  private static final Logger LOGGER = LoggerFactory.getLogger(MoveOperation.class);

  private final Path destination;

  public MoveOperation(Path destination) {
    this.destination = destination;
  }

  public void prepare(Path sourcePath) throws IOException {
    Path mark = destination.resolve(MARK_FILE);
    if (mark.toFile().exists()) {
      Files.write(mark, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
    } else {
      Files.createFile(mark);
    }
    List<String> list = new ArrayList<>();
    try (Stream<Path> paths = Files.walk(sourcePath)) {
      paths.forEach(path -> list.add(path.toString()));
    }
    Files.write(mark, list);
  }

  public void move() {
    Path mark = destination.resolve(MARK_FILE);
    if (mark.toFile().exists()) {
      if (Files.isReadable(mark)) {
        try {
          List<String> allPaths = Files.readAllLines(mark);
          String line = allPaths.get(0);
          Path sourcePath = Paths.get(line);
          Path sourceParentPath = sourcePath.getParent();
          allPaths.forEach(path -> {
            if (!Files.isReadable(Paths.get(path))) {
              String errMsg = "Moving data directory from: " + sourcePath + " to: " + destination +
                  " failed because file: " + path + " is not readable.";
              throw new UncheckedIOException(new IOException(errMsg));
            }
          });

          Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attributes) {

              try {
                Path targetFile = destination.resolve(sourceParentPath.relativize(file));
                org.terracotta.utilities.io.Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
              } catch (IOException ex) {
                String errMsg = "Moving data directory from: " + sourcePath + " to: " + destination +
                    " failed because file: " + file + " cannot be copied.";
                throw new UncheckedIOException(errMsg, new IOException(ex));
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                                                     BasicFileAttributes attributes) {
              Path newDir = null;
              try {
                newDir = destination.resolve(sourceParentPath.relativize(dir));
                if (!Files.isReadable(newDir)) {
                  Files.createDirectory(newDir);
                }
              } catch (IOException ex) {
                String errMsg = "Moving data directory from: " + sourcePath + " to: " + destination +
                    " failed because directory: " + newDir + " cannot be created";
                throw new UncheckedIOException(errMsg, new IOException(ex));
              }
              return FileVisitResult.CONTINUE;
            }
          });
          org.terracotta.utilities.io.Files.delete(mark);
          try {
            org.terracotta.utilities.io.Files.deleteTree(sourcePath);
          } catch (IOException e) {
            LOGGER.warn("Unable to delete obsolete data-dir path: {}", sourcePath);
          }
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      } else {
        throw new RuntimeException("Server cannot startup because movement for one of the " +
            "existing data-dir or metadata-dir to new path: " + destination +
            "cannot be completed because of permission issues");
      }
    }
  }
}

