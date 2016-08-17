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

import com.tc.classloader.CommonComponent;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public interface IMonitoringProducer extends org.terracotta.monitoring.IMonitoringProducer {

  //TODO: Remove this method when tc-apis will be relased and will contain this method
  //See commit: https://github.com/Terracotta-OSS/terracotta-apis/commit/e60f3713d4e8c062c6f983bee091a6b0150bb1bb
  /**
   * Push some data for consumption on the underlying structure matching this tree path
   *
   * @param category category name where to push the data
   */
  void pushBestEffortsData(String category, Object data);
}
