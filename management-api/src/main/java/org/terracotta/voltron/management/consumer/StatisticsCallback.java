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
package org.terracotta.voltron.management.consumer;

import org.terracotta.management.stats.Statistic;

import java.util.Collection;

/**
 * Required interface for the consumers to collect statistics periodically.
 * <p>
 * Consumer can implement this interface in order to get the stats periodically
 * and push it to the remote management system.
 * <p>
 * This must be implemented carefully to avoid packet flooding between stripes and
 * management systems. If the pushStats is called and an ack is pending for the
 * previous pushed stats, return false to indicate that the stats were not pushed.
 *
 * @author RKAV
 */
public interface StatisticsCallback {
  /**
   * Invokes the callback periodically when two conditions are met.
   *   1. Statistics is available in the {@code buffer}
   *   2. The interval specified when this {@code StatisticsCallback} was setup has elapsed.
   * <p>
   * The Statistics is cleared in the {@code buffer} once it is pushed and is no longer available.
   *
   * @param bufferedStats the stats that were buffered.
   * @return true, if the stats was send out (ideally asynchronously) on the pipe and false if the stats
   *  nwas not send out on the pipe (for example, the previous push has not received an ack from the management system).
   */
  boolean pushStats(Collection<Statistic<?, ?>> bufferedStats);
}