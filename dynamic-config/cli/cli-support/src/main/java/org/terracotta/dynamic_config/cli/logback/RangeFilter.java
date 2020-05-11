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
package org.terracotta.dynamic_config.cli.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * @author Mathieu Carbou
 */
public class RangeFilter extends Filter<ILoggingEvent> {

  private volatile int min = Level.TRACE.levelInt;
  private volatile int max = Level.ERROR.levelInt;

  public void setMin(String min) {
    this.min = Level.toLevel(min).levelInt;
  }

  public void setMax(String max) {
    this.max = Level.toLevel(max).levelInt;
  }

  @Override
  public FilterReply decide(ILoggingEvent event) {
    int level = event.getLevel().levelInt;
    return level >= min && level <= max ? FilterReply.NEUTRAL : FilterReply.DENY;
  }
}
