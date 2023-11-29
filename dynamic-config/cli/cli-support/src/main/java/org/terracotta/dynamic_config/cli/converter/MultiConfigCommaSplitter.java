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
package org.terracotta.dynamic_config.cli.converter;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.IParameterSplitter;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.cli.api.command.ConfigurationInput;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * A splitter which splits the input about a comma, and transforms it into a multi-config representation.
 * <p>
 * The splitter is only applied in case of a map setting ({@link Setting#isMap() == true}
 * <p>
 * For example, data-dirs=foo:bar,foo2:bar2 becomes {data-dirs.foo=bar, data-dirs.foo2=bar2}
 * -setting stripe:backup-dir=foo,node1:name=bar
 * -setting stripe:backup-dir=foo -setting node1:name=bar
 *
 * stripe:backup-dir=foo
 * (with or without translation)
 * * Configuration object
 *
 * -setting data-dirs=foo:bar,foo2:bar2
 * -setting data-dirs.foo=bar -setting data-dirs.foo2=bar2
 * -setting data-dirs.foo,data-dirs
 */
public class MultiConfigCommaSplitter implements IParameterSplitter {
  public List<String> split(String value) {

    String[] keyValue = value.split("=", 2);

    if (keyValue.length != 2) {
      // let the next code parse the value and do the correct checks
      return Collections.singletonList(value);
    }

    if (!isMap(keyValue[0])) {
      // let the next code parse the value and do the correct checks if this setting is not a map
      return Collections.singletonList(value);
    }

    if (hasKey(keyValue[0])) {
      // if we have a key defined, there cannot be multiple settings separated with comma in the value
      return Collections.singletonList(value);
    }

    // here we only have inputs like:
    // - data-dirs=foo:bar
    // - data-dirs=foo:bar,foo2:bar2
    // or invalid things like:
    // - data-dirs=foo:
    // - data-dirs=:
    // - data-dirs=d
    // - etc
    return Stream.of(keyValue[1].split(","))
      .filter(s -> !s.trim().isEmpty()) // in case user sends a lot of commas: data-dirs=,foo:bar,,foo2:bar2,,
      .map(s -> {
        String[] nameProperty = s.trim().split(":", 2);
        if (nameProperty.length != 2) {
          // we always expect a key and value for maps
          throw new ParameterException("Invalid input: " + value);
        }
        return keyValue[0] + "." + nameProperty[0] + "=" + nameProperty[1];
      })
      .distinct() // in case user sends: data-dirs=foo:bar,foo:bar
      .collect(toList());
  }

  // remove dependency on Configuration class in this class

  private boolean isMap(String key) {
    try {
      // try to get the setting name and see if this is a map
      return ConfigurationInput.getSetting(key).isMap();
    } catch (RuntimeException e) {
      // if we fail parsing the setting name, consider this is not a map
      return false;
    }
  }

  private boolean hasKey(String key) {
    try {
      // check if a key has been defined.
      // Ie: data-dirs.key=bar
      return ConfigurationInput.getKey(key) != null;
    } catch (RuntimeException e) {
      return false;
    }
  }
}
