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
package org.terracotta.dynamic_config.cli.upgrade_tools.config_converter;

import com.beust.jcommander.converters.EnumConverter;

public enum ConversionFormat {
  DIRECTORY,
  PROPERTIES;

  public static class FormatConverter extends EnumConverter<ConversionFormat> {
    public FormatConverter() {
      super("-t", ConversionFormat.class);
    }
  }
}
