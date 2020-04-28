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
package org.terracotta.dynamic_config.server.configuration.startup;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.WrappedParameter;
import org.terracotta.dynamic_config.api.model.Setting;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static java.lang.System.lineSeparator;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_NAME;

public class CustomJCommander extends JCommander {
  private final Collection<String> userSpecifiedOptions = new HashSet<>();

  public CustomJCommander(Options object) {
    super(object);

    // Hacky way to get the specified options, since JCommander doesn't provide this functionality out-of-the-box
    addConverterInstanceFactory((parameter, forType, optionName) -> {
      userSpecifiedOptions.addAll(Arrays.asList(parameter.names()));
      return null;
    });
  }

  public Collection<String> getUserSpecifiedOptions() {
    return Collections.unmodifiableCollection(userSpecifiedOptions);
  }

  @Override
  public void usage(StringBuilder out, String indent) {
    appendOptions(this, out, indent);
  }

  @Override
  public Map<String, JCommander> getCommands() {
    // force an ordering of commands by name
    return new TreeMap<>(super.getCommands());
  }

  private void appendOptions(JCommander jCommander, StringBuilder out, String indent) {
    List<ParameterDescription> sorted = jCommander.getParameters();
    sorted.sort(Comparator.comparing(ParameterDescription::getLongestName));
    int maxParamLength = sorted.stream().map(pd -> pd.getNames().length()).max(Integer::compareTo).get();

    // Display all the names and descriptions
    if (sorted.size() > 0) {
      out.append(indent).append("Dynamic Configuration Options:").append(lineSeparator());
      for (ParameterDescription pd : sorted) {
        if (pd.getParameter().hidden()) continue;

        WrappedParameter parameter = pd.getParameter();
        out.append(indent).append("    ").append(pd.getNames()).append(parameter.required() ? " (required)" : "");
        Optional<Setting> settingOptional = Setting.findSetting(ConsoleParamsUtils.stripDashDash(pd.getLongestName()));
        if (settingOptional.isPresent()) {
          Setting setting = settingOptional.get();
          String defaultValue = setting.getDefaultValue();

          // special handling
          if (setting == NODE_NAME) {
            defaultValue = "<randomly-generated>";
          }

          if (defaultValue != null) {
            out.append(indent).append("    ");
            for (int i = 0; i < maxParamLength - pd.getNames().length(); i++) {
              out.append(" ");
            }
            out.append("(Default: ").append(defaultValue).append(")");
          }
        }
        out.append(lineSeparator());
      }
    }
  }
}
