/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.lease;

import java.util.concurrent.TimeUnit;

public class TestTimeSource implements TimeSource {
  private volatile long time = 1L;

  public void tickNanos(long increment) {
    time += increment;
  }

  public void tickMillis(long increment) {
    time += TimeUnit.NANOSECONDS.convert(increment, TimeUnit.MILLISECONDS);
  }

  @Override
  public long nanoTime() {
    return time;
  }

  @Override
  public void sleep(long milliseconds) throws InterruptedException {
    long now = time;
    long end = now + TimeUnit.NANOSECONDS.convert(milliseconds, TimeUnit.MILLISECONDS);

    while (time - end < 0) {
      Thread.sleep(10);
    }
  }
}
