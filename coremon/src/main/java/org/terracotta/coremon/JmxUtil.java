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
package org.terracotta.coremon;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Map;

public class JmxUtil {

  private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
  private final ObjectName internalTerracottaServerObjectName;

  public JmxUtil() throws Exception {
    this.internalTerracottaServerObjectName = new ObjectName("org.terracotta:name=TerracottaServer");
  }

  public Map<String, Object> getStatistics() throws Exception {
    return (Map<String, Object>) mBeanServer.getAttribute(internalTerracottaServerObjectName, "Statistics");
  }

}
