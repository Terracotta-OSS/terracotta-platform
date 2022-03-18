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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
public class Binary {

  private static final Logger LOGGER = LoggerFactory.getLogger(Binary.class);
  private static final boolean WIN = System.getProperty("os.name", "").toLowerCase().contains("win");

  public static final Path JPS;
  public static final Path JSTACK;

  static {
    JPS = find("jps").orElse(null);
    JSTACK = find("jstack").orElse(null);
    if (JPS == null || JSTACK == null) {
      LOGGER.warn("Unable to find jps or jstack location using java.home ({}) or JAVA_HOME ({}))", System.getProperty("java.home"), System.getenv("JAVA_HOME"));
    } else {
      LOGGER.trace("jps: {}", JPS);
      LOGGER.trace("jstack: {}", JSTACK);
    }
  }

  public static Optional<Path> find(String name) {
    return Stream.of(System.getProperty("java.home"), System.getenv("JAVA_HOME"))
        .filter(Objects::nonNull)
        .map(Paths::get)
        .flatMap(home -> Stream.of(home, home.getParent())) // second entry will be the jdk if home points to a jre
        .map(home -> home.resolve("bin").resolve(bin(name)))
        .filter(Files::exists)
        .findFirst();
  }

  public static String exec(Path bin, String... params) {
    String[] cmd = new String[params.length + 1];
    cmd[0] = bin.toString();
    System.arraycopy(params, 0, cmd, 1, params.length);
    try {
      LOGGER.trace("exec({})", String.join(" ", cmd));
      Process process = new ProcessBuilder(cmd)
          .redirectErrorStream(true)
          .start();
      process.waitFor();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try (InputStream is = process.getInputStream()) {
        int b;
        while ((b = is.read()) != -1) {
          out.write(b);
        }
      }
      return out.toString("UTF-8");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static String exec(Duration timeout, Path bin, String... params) throws TimeoutException {
    String[] cmd = new String[params.length + 1];
    cmd[0] = bin.toString();
    System.arraycopy(params, 0, cmd, 1, params.length);
    try {
      LOGGER.trace("exec({}, {})", timeout, String.join(" ", cmd));
      Process process = new ProcessBuilder(cmd)
          .redirectErrorStream(true)
          .start();
      if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        process.waitFor();
        throw new TimeoutException("Timeout after " + timeout.getSeconds() + " seconds when trying to execute: " + String.join(" ", cmd));
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try (InputStream is = process.getInputStream()) {
        int b;
        while ((b = is.read()) != -1) {
          out.write(b);
        }
      }
      return out.toString("UTF-8");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static String bin(String name) {
    return WIN ? name + ".exe" : name;
  }
}
