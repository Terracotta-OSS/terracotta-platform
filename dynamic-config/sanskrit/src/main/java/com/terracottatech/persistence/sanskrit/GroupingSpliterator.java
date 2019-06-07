/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Batches up a Stream<String> into Stream<Deque<String>>. An empty string is the batch separator.
 * This is useful for parsing the append.log file because empty lines delimit the records.
 * Note that the final batch will be the last one terminated by a batch separator - any lines after that are ignored.
 */
public class GroupingSpliterator implements Spliterator<Deque<String>> {
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
        if (line.equals("")) {
          batchFull.set(true);
        } else {
          batch.add(line);
        }
      });

      if (!more || batchFull.get()) {
        stopBatch = true;
      }
    }

    if (!batchFull.get()) {
      return false;
    }

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
