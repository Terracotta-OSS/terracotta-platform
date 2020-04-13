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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.org.junit.rules.TemporaryFolder;

import java.nio.file.Path;

/**
 * @author Mathieu Carbou
 */
public class TmpDir implements TestRule {

  private static final Logger LOGGER = LoggerFactory.getLogger(TmpDir.class);

  private final Path parent;
  private final boolean autoClean;
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
  public Statement apply(Statement base, Description description) {
    TemporaryFolder temporaryFolder = new TemporaryFolder(parent == null ? null : parent.toFile()) {
      @Override
      protected void after() {
        if (autoClean) {
          super.after();
        }
      }
    };
    return temporaryFolder.apply(new Statement() {
      @Override
      public void evaluate() throws Throwable {
        if (description.getMethodName() == null) {
          root = temporaryFolder.newFolder(description.getTestClass().getSimpleName()).toPath();
          LOGGER.info("Temporary directory for {}: {}", description.getTestClass().getSimpleName(), root);
        } else {
          root = temporaryFolder.newFolder(description.getTestClass().getSimpleName(), description.getMethodName()).toPath();
          LOGGER.info("Temporary directory for {}#{}: {}", description.getTestClass().getSimpleName(), description.getMethodName(), root);
        }
        base.evaluate();
      }
    }, description);
  }

  public Path getRoot() {
    return root;
  }
}
