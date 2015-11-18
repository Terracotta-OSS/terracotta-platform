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
 *  The Covered Software is Entity Management API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.voltron.management.common.providers;

import java.util.Collection;
import java.util.Map;

/**
 * Statistics provider provided by the managed object. This is a required interface that any entity that has
 * managed objects must implement in order to control the amount of statistics being pushed.
 *
 * @author RKAV
 */
public interface StatisticsProvider<T> extends ManagementProvider<T> {
  /**
   * Start pushing the statistics, designated by the collection of statisticNames.
   * <p>
   * Once this is called, the provider MUST start pushing the statistics.
   *
   * @param context the {@code context} that describes the managed objects for which statistics collection must start.
   * @param statisticNames denotes the statistics that should be pushed by the entity after this call.
   */
  void startCollection(Map<String, String> context, Collection<String> statisticNames);

  /**
   * Stop pushing the statistics, designated by the collection of statisticNames.
   * <p>
   * Once this is called, the provider MUST stop pushing the statistics for these set of stats.
   *
   * @param context the {@code context} that describes the managed objects for which statistics collection must stop.
   * @param statisticNames denotes the currently pushed statistics that should be stopped from being pushed by the entity.
   */
  void stopCollection(Map<String, String> context, Collection<String> statisticNames);
}