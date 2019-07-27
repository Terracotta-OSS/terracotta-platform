/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test.util;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Mathieu Carbou
 */
public class TmpDir implements TestRule {

  private static final Logger LOGGER = LoggerFactory.getLogger(TmpDir.class);

  private static final String TIME = LocalDateTime.now()
      .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      .replace(":", "-");

  private final Path parent;
  private Path root;

  public TmpDir(Path parent) {
    this.parent = parent;
  }

  public TmpDir() {
    this(Paths.get(System.getProperty("user.dir"), "build", "test-data").toAbsolutePath());
  }

  @Override
  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        createRoot(description);
        base.evaluate();
      }
    };
  }

  public Path getRoot() {
    return root;
  }

  private void createRoot(Description description) throws IOException {
    String cname = description.getTestClass().getSimpleName();
    String mname = description.getMethodName();
    if (mname == null) {
      mname = "_static_"; // if the rule is set as being static
    }
    root = parent.resolve(TIME).resolve(cname).resolve(mname);
    Files.createDirectories(root);
    LOGGER.info("Temporary directory created for test: " + root);
  }
}
