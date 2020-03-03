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
package org.terracotta.dynamic_config.system_tests.util;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.Map;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.ORDERED;

/**
 * This Junit rule can be used to split the Angela logging sent to System.out in different log buckets per node.
 * <p>
 * This is a temporary hack and this feature should be in Angela itself.
 *
 * TODO [DYNAMIC-CONFIG]: TDB-4862 - have the logs per process in Angela (https://github.com/Terracotta-OSS/angela/issues/16)
 *
 * @author Mathieu Carbou
 */
public class NodeOutputRule implements TestRule {

  private static final String DEFAULT_ENCODING = Charset.defaultCharset().name();

  private final Map<String, NodeLog> outputs = new ConcurrentHashMap<>();

  public Statement apply(Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        PrintStream original = System.out;
        try {
          System.setOut(new PrintStream(new LineOutputStream(original, 1024, line -> onNewLine(line)), true, DEFAULT_ENCODING));
          base.evaluate();
        } finally {
          System.setOut(original);
        }
      }
    };
  }

  public NodeLog getLog(int stripeId, int nodeId) {
    return outputs.computeIfAbsent(stripeId + "-" + nodeId, key -> new NodeLog());
  }

  public void clearLog(int stripeId, int nodeId) {
    getLog(stripeId, nodeId).clearLog();
  }

  public void clearLog() {
    outputs.values().forEach(NodeLog::clearLog);
  }

  private void onNewLine(String line) {
    // Angela log lines are standard:
    // 2020-02-26 08:19:37.894 INFO  o.t.a.e.tsa:98 - [node-1-1] xyz
    // 2020-02-26 08:19:38.640 INFO  o.t.a.e.tsa:98 - [node-1-1] xyz
    final int start = line.indexOf(" - [node-");
    if (start != -1) {
      final int middle = line.indexOf('-', start + 9);
      if (middle != -1) {
        final int end = line.indexOf(']', middle + 1);
        if (end != -1) {
          final int stripeId = Integer.parseInt(line.substring(start + 9, middle));
          final int nodeId = Integer.parseInt(line.substring(middle + 1, end));
          getLog(stripeId, nodeId).append(line);
        }
      }
    }
  }

  private static class LineOutputStream extends OutputStream {
    private static final byte EOL = (byte) '\n';

    private final OutputStream next;
    private final ByteBuffer buffer;
    private final Consumer<String> onNewLine;

    public LineOutputStream(OutputStream next, int maximumLineLength, Consumer<String> onNewLine) {
      this.onNewLine = onNewLine;
      this.next = next;
      this.buffer = ByteBuffer.allocate(maximumLineLength);
    }

    @Override
    public void write(int b) throws IOException {
      next.write(b);
      // when we reach EOL, we check the buffer
      if (b == EOL) {
        String line = new String(buffer.array(), 0, buffer.position(), DEFAULT_ENCODING) + "\n";
        onNewLine.accept(line);
        buffer.clear();
      } else if (buffer.hasRemaining()) {
        // we only continue filling the buffer if we have some space left
        buffer.put((byte) b);
      }
    }

    @Override
    public void close() throws IOException {
      this.next.close();
    }
  }

  public static class NodeLog {
    private final Deque<String> logs = new ConcurrentLinkedDeque<>();

    public Stream<String> streamLogs() {
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(logs.iterator(), ORDERED), false);
    }

    public Stream<String> streamLogsDescending() {
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(logs.descendingIterator(), ORDERED), false);
    }

    public void clearLog() {
      logs.clear();
    }

    private void append(String line) {
      logs.offer(line);
    }

    @Override
    public String toString() {
      return String.join("", logs);
    }
  }
}
