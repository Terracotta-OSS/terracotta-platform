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
package org.terracotta.management.sequence;

import com.tc.classloader.CommonComponent;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public interface TimeSource {

  TimeSource SYSTEM = Defaults.SYSTEM_TIME_SOURCE;
  TimeSource BEST = Defaults.BEST_TIME_SOURCE;

  long getTimestamp();

  class Fixed implements TimeSource {

    private final long timestamp;

    public Fixed(long timestamp) {
      this.timestamp = timestamp;
    }

    @Override
    public long getTimestamp() {
      return timestamp;
    }
  }
}
