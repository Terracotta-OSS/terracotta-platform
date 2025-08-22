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
package org.terracotta.management.service.monitoring;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * @author Mathieu Carbou
 */
class ExecutionChain<T> {

  private final Queue<TaggedConsumer<T>> chain = new ConcurrentLinkedQueue<>();

  private volatile boolean done;
  private volatile T value;

  // execute if possible or discard the execution
  void executeOrDiscard(Consumer<T> consumer) {
    if (done) {
      consumer.accept(value);
    }
  }

  // enqueue future executions
  void executeOrDelay(Consumer<T> consumer) {
    if (done) {
      consumer.accept(value);
    } else {
      chain.offer(new TaggedConsumer<>(consumer));
    }
  }

  // enqueue future execution but remove those with the same tag that were there before
  void executeOrDelay(String tag, Consumer<T> consumer) {
    if (done) {
      consumer.accept(value);
    } else {
      TaggedConsumer<T> tc = new TaggedConsumer<>(tag, consumer);
      chain.removeIf(taggedConsumer -> tag.equals(taggedConsumer.tag));
      chain.offer(tc);
    }
  }

  void complete(T t) {
    value = t; // we allow the value to be reset
    done = true;
    while (!chain.isEmpty()) {
      chain.poll().consumer.accept(t);
    }
  }

  private static final class TaggedConsumer<T> {
    final String tag;
    final Consumer<T> consumer;

    TaggedConsumer(Consumer<T> consumer) {
      this.tag = null;
      this.consumer = Objects.requireNonNull(consumer);
    }

    TaggedConsumer(String tag, Consumer<T> consumer) {
      this.tag = Objects.requireNonNull(tag);
      this.consumer = Objects.requireNonNull(consumer);
    }
  }
}
