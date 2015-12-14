/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity Management Service.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.management.service.buffer;

import org.terracotta.management.context.Context;
import org.terracotta.management.stats.ContextualStatistics;
import org.terracotta.management.stats.MemoryUnit;
import org.terracotta.management.stats.Statistic;
import org.terracotta.management.stats.primitive.Average;
import org.terracotta.management.stats.primitive.Size;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Contextual statistics in ring buffer.
 *
 * @author RKAV
 */
public abstract class BaseStatisticsBufferTest extends BaseBufferTest<ContextualStatistics> {
  @Override
  protected ContextualStatistics getOneItem() {
    return fillContextualStatistics();
  }

  @Override
  protected Class<ContextualStatistics[]> getArrayType() {
    return ContextualStatistics[].class;
  }

  private ContextualStatistics fillContextualStatistics() {
    Context context = Context.create("test", "test");
    Map<String, Statistic<?, ?>> statsMap = new HashMap<>();
    statsMap.put("testStats1", new Size("Memory Size", 100L, MemoryUnit.KB));
    statsMap.put("testStats2", new Average("CPU Usage System", 1.0, TimeUnit.HOURS));
    statsMap.put("testStats3", new Size("Disk Size", 10L, MemoryUnit.TB));
    return new ContextualStatistics(context, statsMap);
  }
}
