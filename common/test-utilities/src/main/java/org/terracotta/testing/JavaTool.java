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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.terracotta.testing.JavaBinary.JMAP;
import static org.terracotta.testing.JavaBinary.JPS;
import static org.terracotta.testing.JavaBinary.JSTACK;
import static org.terracotta.testing.JavaBinary.exec;

/**
 * @author Mathieu Carbou
 */
public class JavaTool {

  private static final Logger LOGGER = LoggerFactory.getLogger(JavaTool.class);

  public static Stream<Process> processes() {
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
          return new Process(Long.parseLong(line.substring(0, sep)), name.isEmpty() ? "unknown" : name);
        });
  }

  public static Optional<Dump> threadDump(Process process, Duration timeout) {
    LOGGER.info("Taking thread dump of process: {} (timeout: {})", process, timeout == null ? "none" : timeout.toMillis() + "ms");
    String dump = exec(timeout, JSTACK, "-l", "" + process.pid);
    if (dump.contains("No such process")) {
      LOGGER.warn("No such process: {}", process);
      return Optional.empty();
    } else {
      return Optional.of(new Dump(process, dump));
    }
  }

  public static Stream<Dump> threadDumps(Duration timeout) {
    return processes()
        .map(p -> threadDump(p, timeout))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  public static void threadDumps(Path outputDir, Duration timeout) {
    try {
      Files.createDirectories(outputDir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    threadDumps(timeout).forEach(dump -> dump.writeTo(outputDir.resolve("thread-dump-" + dump.process.pid + "-" + dump.process.processName.replaceAll("\\W", "_") + ".log")));
  }

  public static void memoryDump(Process process, Path output, Duration timeout) {
    LOGGER.info("Taking memory dump of process: {} (timeout: {})", process, timeout == null ? "none" : timeout.toMillis() + "ms");
    exec(timeout, JMAP, "-dump:format=b,file=" + output, "" + process.pid);
  }

  public static void memoryDumps(Path outputDir, Duration timeout) {
    try {
      Files.createDirectories(outputDir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    processes().forEach(p -> memoryDump(p, outputDir.resolve("memory-dump-" + p.pid + "-" + p.processName.replaceAll("\\W", "_") + ".bin"), timeout));
  }

  public static class Dump {
    private final Process process;
    private final String dump;

    private Dump(Process process, String dump) {
      this.process = process;
      this.dump = requireNonNull(dump);
    }

    public Process getProcess() {
      return process;
    }

    public String getDump() {
      return dump;
    }

    public void writeTo(Path out) {
      LOGGER.info("Saving dump of process: {} to: {}", process, out);
      try {
        Files.write(out, dump.getBytes(StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  public static class Process {
    private final long pid;
    private final String processName;

    private Process(long pid, String processName) {
      this.pid = pid;
      this.processName = processName == null || processName.trim().isEmpty() ? "unknown" : processName.trim();
    }

    public long getPid() {
      return pid;
    }

    public String getProcessName() {
      return processName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Process that = (Process) o;
      return pid == that.pid;
    }

    @Override
    public int hashCode() {
      return Objects.hash(pid);
    }

    @Override
    public String toString() {
      return pid + " (" + processName + ")";
    }
  }
}
