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
package org.terracotta.management.service.monitoring;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * @author Mathieu Carbou
 */
class ExecutionChain<T> {

  private final Queue<Consumer<T>> chain = new ConcurrentLinkedQueue<>();

  private volatile boolean done;
  private volatile T value;

  void execute(Consumer<T> consumer) {
    if (done) {
      consumer.accept(value);
    } else {
      chain.offer(consumer);
    }
  }

  void complete(T t) {
    value = t; // we allow the value to be reset
    done = true;
    while (!chain.isEmpty()) {
      chain.poll().accept(t);
    }
  }

}
