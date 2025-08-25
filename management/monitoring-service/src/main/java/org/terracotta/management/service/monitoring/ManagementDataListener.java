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

import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.cluster.ManagementRegistry;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;

/**
 * Class returned to the platform so that we can be called back with the data coming from a passive entity (from a DefaultEntityMonitoringService)
 *
 * @author Mathieu Carbou
 */
interface ManagementDataListener {

  void onStatistics(MessageSource messageSource, ContextualStatistics[] statistics);

  void onCallAnswer(MessageSource messageSource, String managementCallIdentifier, ContextualReturn<?> answer);

  void onRegistry(MessageSource messageSource, ManagementRegistry registry);

  void onNotification(MessageSource messageSource, ContextualNotification notification);

}
