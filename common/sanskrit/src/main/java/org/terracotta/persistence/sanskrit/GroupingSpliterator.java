/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.persistence.sanskrit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Batches up a {@code Stream<String>} into {@code Stream<Deque<String>>}. An empty string is the batch separator.
 * This is useful for parsing the append.log file because empty lines delimit the records.
 * Note that the final batch will be the last one terminated by a batch separator - any lines after that are ignored.
 */
public class GroupingSpliterator implements Spliterator<Deque<String>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(GroupingSpliterator.class);
  private final Spliterator<String> lines;

  public GroupingSpliterator(Stream<String> lines) {
    this(lines.spliterator());
  }

  public GroupingSpliterator(Spliterator<String> lines) {
    this.lines = lines;
  }

  @Override
  public boolean tryAdvance(Consumer<? super Deque<String>> action) {
    Deque<String> batch = new ArrayDeque<>();

    boolean stopBatch = false;
    AtomicBoolean batchFull = new AtomicBoolean();

    while (!stopBatch) {
      boolean more = lines.tryAdvance(line -> {
        if (line.isEmpty()) {
          batchFull.set(true);
          LOGGER.trace("end of batch");
        } else {
          batch.add(line);
          LOGGER.trace("new line: {}", line.replace("\r", "\\r").replace("\n", "\\n"));
        }
      });

      if (!more || batchFull.get()) {
        stopBatch = true;
      }
    }

    if (!batchFull.get()) {
      LOGGER.trace("no more lines");
      return false;
    }

    LOGGER.trace("batch: {}", batch);
    action.accept(batch);

    return true;
  }

  @Override
  public Spliterator<Deque<String>> trySplit() {
    return null;
  }

  @Override
  public long estimateSize() {
    return Long.MAX_VALUE;
  }

  @Override
  public int characteristics() {
    return ORDERED | NONNULL | IMMUTABLE;
  }
}
