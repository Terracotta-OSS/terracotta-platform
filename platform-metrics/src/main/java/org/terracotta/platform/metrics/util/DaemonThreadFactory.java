/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2026
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.terracotta.platform.metrics.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A {@link ThreadFactory} that produces daemon threads.
 *
 * <p>Every thread created has its daemon flag set to {@code true}, ensuring it does not
 * prevent JVM shutdown. Threads are named {@code "<group>-<n>"} where {@code n} is a
 * monotonically increasing counter per factory instance.
 */
public class DaemonThreadFactory implements ThreadFactory {
  private final AtomicLong count = new AtomicLong();
  private final String group;
  private final ThreadGroup grp;

  /**
   * Creates a factory that places threads in the default thread group.
   *
   * @param group thread name prefix
   */
  public DaemonThreadFactory(String group) {
    this(group, null);
  }

  /**
   * Creates a factory that places threads in the given thread group.
   *
   * @param group thread name prefix
   * @param grp   thread group to assign to new threads, or {@code null} for the default group
   */
  public DaemonThreadFactory(String group, ThreadGroup grp) {
    this.group = group;
    this.grp = grp;
  }

  @Override
  public Thread newThread(Runnable r) {
    Thread t = new Thread(grp, r, group + "-" + count.incrementAndGet());
    t.setDaemon(true);
    return t;
  }

  @Override
  public String toString() {
    return group;
  }
}
