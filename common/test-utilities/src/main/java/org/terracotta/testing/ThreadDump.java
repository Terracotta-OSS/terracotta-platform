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
  private final String output;

  private ThreadDump(long pid, String output) {
    this.pid = pid;
    this.output = requireNonNull(output);
  }

  public long getPid() {
    return pid;
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
    return Stream.of(exec(JPS, "-q").split("\\r?\\n"))
        .map(Long::parseLong)
        .map(pid -> dump(pid, timeout))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  public static void dumpAll(Path outputDir) {
    dumpAll(outputDir, false, DEFAULT_TIMEOUT);
  }

  public static void dumpAll(Path outputDir, boolean parallel, Duration timeout) {
    try {
      Files.createDirectories(outputDir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    Stream<ThreadDump> stream = dumpAll(timeout);
    if (parallel) {
      stream = stream.parallel();
    }
    stream.forEach(dump -> dump.writeTo(outputDir.resolve("thread-dump-" + dump.getPid() + ".log")));
  }

  public static Optional<ThreadDump> dump(long pid) {
    return dump(pid, DEFAULT_TIMEOUT);
  }

  public static Optional<ThreadDump> dump(long pid, Duration timeout) {
    LOGGER.info("Taking thread dump of PID {} (timeout: " + (timeout == null ? "none" : timeout.toMillis() + "ms") + ")", pid);
    String output = exec(timeout, JSTACK, "-l", "" + pid);
    if (output.contains("No such process")) {
      LOGGER.warn("No such process: {}", pid);
      return Optional.empty();
    } else {
      return Optional.of(new ThreadDump(pid, output));
    }
  }

  public static void dump(long pid, Path out) {
    dump(pid).ifPresent(dump -> dump.writeTo(out));
  }
}
