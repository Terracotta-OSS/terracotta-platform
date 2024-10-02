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
package org.terracotta.testing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Mathieu Carbou
 */
@SuppressFBWarnings("ENV_USE_PROPERTY_INSTEAD_OF_ENV")
public class JavaBinary {

  private static final Logger LOGGER = LoggerFactory.getLogger(JavaBinary.class);
  private static final boolean WIN = System.getProperty("os.name", "").toLowerCase().contains("win");

  public static final Path JPS;
  public static final Path JSTACK;
  public static final Path JMAP;

  static {
    JPS = find("jps").orElse(null);
    JSTACK = find("jstack").orElse(null);
    JMAP = find("jmap").orElse(null);
    if (JPS == null || JSTACK == null || JMAP == null) {
      LOGGER.warn("Unable to find jps or jstack or jmap location using java.home ({}) or JAVA_HOME ({}))", System.getProperty("java.home"), System.getenv("JAVA_HOME"));
    } else {
      LOGGER.trace("jps: {}", JPS);
      LOGGER.trace("jstack: {}", JSTACK);
      LOGGER.trace("jmap: {}", JMAP);
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

  public static String exec(Duration timeout, Path bin, String... params) {
    final long delayMs = Math.max(timeout == null ? 0 : timeout.toMillis(), 0);
    String[] cmd = new String[params.length + 1];
    cmd[0] = bin.toString();
    System.arraycopy(params, 0, cmd, 1, params.length);

    LOGGER.trace("exec(timeout={}ms, cmd={})", delayMs, String.join(" ", cmd));

    // start the process
    Process process;
    try {
      process = new ProcessBuilder(cmd)
          .redirectErrorStream(true)
          .start();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    // output
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    // if the process does not end within the specified duration, kills it
    Timer timer = null;
    if (delayMs > 0) {
      timer = new Timer();
      timer.schedule(new TimerTask() {
        @Override
        public void run() {
          if (process.isAlive()) {
            String message = "Timeout after " + timeout.getSeconds() + " seconds when trying to execute: " + String.join(" ", cmd);
            try {
              out.write(message.getBytes(UTF_8));
            } catch (IOException e) {
              LOGGER.warn(message, e);
            }
            process.destroyForcibly();
          }
        }
      }, timeout.toMillis());
    }

    // reads the stream until the process ends
    try (InputStream is = process.getInputStream()) {
      int b;
      while ((b = is.read()) != -1) {
        out.write(b);
      }
    } catch (IOException e) {
      LOGGER.warn("Unable to read process input stream: {}", e.getMessage(), e);
    }

    // 2 possibilities: process ended correctly or process was destroyed by the timer (only if we wanted a timeout)
    try {
      process.waitFor();
      return out.toString("UTF-8");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    } finally {
      if (timer != null) {
        timer.cancel();
      }
    }
  }

  public static String bin(String name) {
    return WIN ? name + ".exe" : name;
  }
}
