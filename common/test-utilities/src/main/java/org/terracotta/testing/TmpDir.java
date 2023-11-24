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
package org.terracotta.testing;

import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
public class TmpDir extends ExtendedTestRule {

  private static final Logger LOGGER = LoggerFactory.getLogger(TmpDir.class);

  private final Path parent;
  private final boolean autoClean;
  private TemporaryFolder delegate;
  private Path root;

  public TmpDir() {
    this(null, true);
  }

  public TmpDir(Path parent) {
    this(parent, true);
  }

  public TmpDir(boolean autoClean) {
    this(null, autoClean);
  }

  public TmpDir(Path parent, boolean autoClean) {
    this.parent = parent;
    this.autoClean = autoClean;
  }

  @Override
  protected void before(Description description) throws Throwable {
    if (parent != null) {
      Files.createDirectories(parent);
    }
    delegate = TemporaryFolder.builder()
        .parentFolder(parent == null ? null : parent.toFile())
        .assureDeletion()
        .build();
    delegate.create();
    if (description.getMethodName() == null) {
      root = delegate.newFolder(description.getTestClass().getSimpleName()).toPath();
      LOGGER.info("Temporary directory for {}: {}", description.getTestClass().getSimpleName(), root);
    } else {
      root = delegate.newFolder(description.getTestClass().getSimpleName(), description.getMethodName()).toPath();
      LOGGER.info("Temporary directory for {}#{}: {}", description.getTestClass().getSimpleName(), description.getMethodName(), root);
    }
  }

  @Override
  protected void after(Description description) throws Throwable {
    if (autoClean || empty()) {
      delegate.delete();
    }
  }

  public Path getRoot() {
    return root;
  }

  private boolean empty() {
    try (Stream<Path> walk = Files.walk(root)) {
      return walk.allMatch(Files::isDirectory);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
