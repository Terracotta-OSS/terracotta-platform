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

import org.terracotta.monitoring.PlatformServer;

import java.io.Serializable;

/**
 * Class returned to the platform so that we can be called back with the data coming from a passive entity (from a DefaultPassiveEntityMonitoringService)
 *
 * @author Mathieu Carbou
 */
interface DataListener {

  void pushBestEffortsData(long consumerId, PlatformServer sender, String name, Serializable data);

  void setState(long consumerId, PlatformServer sender, String[] path, Serializable data);
}
