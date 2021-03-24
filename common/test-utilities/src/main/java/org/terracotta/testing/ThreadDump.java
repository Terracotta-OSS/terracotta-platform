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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.terracotta.testing.Binary.JPS;
import static org.terracotta.testing.Binary.JSTACK;
import static org.terracotta.testing.Binary.exec;

/**
 * @author Mathieu Carbou
 */
public class ThreadDump {

  public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

  private static final Logger LOGGER = LoggerFactory.getLogger(ThreadDump.class);

  private final long pid;
  private final String name;
  private final String output;

  private ThreadDump(long pid, String name, String output) {
    this.pid = pid;
    this.name = name;
    this.output = requireNonNull(output);
  }

  public long getPid() {
    return pid;
  }

  public String getName() {
    return name;
  }

  public String getOutput() {
    return output;
  }

  public void writeTo(Path out) {
    LOGGER.info("Saving thread dump of PID {} to: {}", pid, out);
    try {
      Files.write(out, output.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static Stream<ThreadDump> dumpAll() {
    return dumpAll(DEFAULT_TIMEOUT);
  }

  public static Stream<ThreadDump> dumpAll(Duration timeout) {
    if (JPS == null) {
      return Stream.empty();
    }
    return Stream.of(exec(JPS).split("\\r?\\n"))
        .filter(line -> !line.trim().isEmpty())
        .filter(line -> !line.toLowerCase(Locale.US).contains("jstack")) // filter out currently stalled or running jstack processes
        .filter(line -> !line.toLowerCase(Locale.US).contains("jps")) // filter out currently running jps
        .filter(line -> !line.toLowerCase(Locale.US).contains("process information unavailable"))
        .map(line -> {
          int sep = line.indexOf(" ");
          String name = (sep == -1 ? "unknown" : line.substring(sep + 1)).trim();
          return dump(Long.parseLong(line.substring(0, sep)), name.isEmpty() ? "unknown" : name, timeout);
        })
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  public static void dumpAll(Path outputDir) {
    dumpAll(outputDir, DEFAULT_TIMEOUT);
  }

  public static void dumpAll(Path outputDir, Duration timeout) {
    try {
      Files.createDirectories(outputDir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    dumpAll(timeout).forEach(dump -> dump.writeTo(outputDir.resolve("thread-dump-" + dump.getPid() + "-" + dump.getName().replaceAll("\\W", "_") + ".log")));
  }

  public static Optional<ThreadDump> dump(long pid, String name) {
    return dump(pid, name, DEFAULT_TIMEOUT);
  }

  public static Optional<ThreadDump> dump(long pid, String name, Duration timeout) {
    LOGGER.info("Taking thread dump of PID {}: '{}' (timeout: {})", pid, name, timeout == null ? "none" : timeout.toMillis() + "ms");
    String output = exec(timeout, JSTACK, "-l", "" + pid);
    if (output.contains("No such process")) {
      LOGGER.warn("No such process: {}", pid);
      return Optional.empty();
    } else {
      return Optional.of(new ThreadDump(pid, name, output));
    }
  }

  public static void dump(long pid, String name, Path out) {
    dump(pid, name).ifPresent(dump -> dump.writeTo(out));
  }
}
