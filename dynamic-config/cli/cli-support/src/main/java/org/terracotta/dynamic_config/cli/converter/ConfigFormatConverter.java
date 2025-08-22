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
package org.terracotta.dynamic_config.cli.converter;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import org.terracotta.dynamic_config.api.model.ConfigFormat;

public class ConfigFormatConverter implements IStringConverter<ConfigFormat> {
  @Override
  public ConfigFormat convert(String value) {
    switch (value) {
      case "name": // backward compatibility with <= 5.8.4 / 10.7.0.2
      case "cfg":
      case "conf":
      case "config":
        return ConfigFormat.CONFIG;
      case "index": // backward compatibility with <= 5.8.4 / 10.7.0.2
      case "properties":
        return ConfigFormat.PROPERTIES;
      case "json": // only used for testing
        return ConfigFormat.JSON;
      default:
        throw new ParameterException("Invalid format: " + value + ". Supported formats: " + String.join(", ", ConfigFormat.supported()));
    }
  }
}
