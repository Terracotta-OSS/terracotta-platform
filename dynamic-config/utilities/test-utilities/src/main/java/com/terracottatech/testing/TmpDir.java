/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.testing;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static java.util.Comparator.reverseOrder;

/**
 * @author Mathieu Carbou
 */
public class TmpDir implements TestRule {

  private static final Logger LOGGER = LoggerFactory.getLogger(TmpDir.class);
  private static final int MAX_RETRY_COUNT = 10;

  private final Path parent;
  private final boolean autoClean;
  private Path root;

  public TmpDir(Path parent, boolean autoClean) {
    this.parent = parent;
    this.autoClean = autoClean;
  }

  public TmpDir() {
    this(Paths.get(System.getProperty("user.dir"), "build", "test-data").toAbsolutePath(), true);
  }

  public TmpDir(Path parent) {
    this(parent, true);
  }

  public TmpDir(boolean autoClean) {
    this(Paths.get(System.getProperty("user.dir"), "build", "test-data").toAbsolutePath(), autoClean);
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        createRoot(description);
        base.evaluate();
        // not in a try catch: we do not clean if test failed
        autoClean();
      }
    };
  }

  public Path getRoot() {
    return root;
  }

  protected void createRoot(Description description) throws IOException {
    // generate a temporary working directory per test that does not depend on
    // class name or test method names because the path can be greater than the
    // limit allowed by Windows.
    Files.createDirectories(parent);
    root = Files.createTempDirectory(parent, System.currentTimeMillis() + "-");
    Files.delete(root);
    Files.createDirectory(root);
    if (LOGGER.isInfoEnabled()) {
      String cname = description.getTestClass().getSimpleName();
      String mname = description.getMethodName();
      if (mname == null) {
        mname = "_static_"; // if the rule is set as being static
      }
      LOGGER.info("Temporary directory for test {}#{}: {}", cname, mname, root);
    }
  }

  protected void autoClean() {
    if (autoClean) {
      for (int i = 0; i < MAX_RETRY_COUNT && Files.exists(root); i++) {
        try (Stream<Path> stream = Files.walk(root)) {
          stream.sorted(reverseOrder()).forEach(path -> {
            try {
              Files.delete(path);
            } catch (IOException e) {
              LOGGER.warn("Error deleting {}", path, e);
            }
          });
        } catch (IOException e) {
          // closing stream
          throw new AssertionError(e);
        }
      }
      if (Files.exists(root)) {
        root.toFile().deleteOnExit();
      }
    }
  }
}
