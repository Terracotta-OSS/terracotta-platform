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
package org.terracotta.dynamic_config.cli.command;

import com.beust.jcommander.Parameters;

/**
 * @author Mathieu Carbou
 */
public class Metadata {

  public static String getName(JCommanderCommand command) {
    Parameters annotation = command.getClass().getAnnotation(Parameters.class);
    if (annotation != null && annotation.commandNames().length > 0) {
      return annotation.commandNames()[0] + (command.getClass().isAnnotationPresent(DeprecatedUsage.class) ? "-deprecated" : "");
    }
    return command.getClass().getSimpleName().toLowerCase().replace("command", "");
  }

  public static String getUsage(JCommanderCommand command) {
    Usage annotation = command.getClass().getAnnotation(Usage.class);
    if (annotation != null) {
      return annotation.value();
    }
    return "";
  }

  public static String getDeprecatedUsage(JCommanderCommand command) {
    DeprecatedUsage annotation = command.getClass().getAnnotation(DeprecatedUsage.class);
    if (annotation != null) {
      return annotation.value();
    }
    return "";
  }
}
