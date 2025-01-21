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
package org.terracotta.dynamic_config.server.configuration.startup;

import org.terracotta.dynamic_config.api.model.Setting;

public class ConsoleParamsUtils {
  public static String stripDashDash(String param) {
    return param.substring(2);
  }

  public static String stripDash(String param) {
    return param.substring(1);
  }

  public static String addDashDash(String param) {
    return "--" + param;
  }

  public static String addDash(String param) {
    return "-" + param;
  }

  public static String addDashDash(Setting param) {
    return addDashDash(param.toString());
  }
}