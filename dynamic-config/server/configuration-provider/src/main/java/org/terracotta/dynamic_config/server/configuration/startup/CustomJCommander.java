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

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.WrappedParameter;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.server.configuration.service.ParameterSubstitutor;
import org.terracotta.dynamic_config.server.configuration.startup.parsing.OptionsParsing;

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
import static org.terracotta.dynamic_config.api.model.Setting.STRIPE_NAME;

public class CustomJCommander extends JCommander {
  private final Collection<String> userSpecifiedOptions = new HashSet<>();

  public CustomJCommander(OptionsParsing object) {
    super(object);
    setUsageFormatter(new UsageFormatter(this));

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
  public Map<String, JCommander> getCommands() {
    // force an ordering of commands by name
    return new TreeMap<>(super.getCommands());
  }


  private static class UsageFormatter extends DefaultUsageFormatter {
    private final JCommander commander;

    private UsageFormatter(JCommander commander) {
      super(commander);
      this.commander = commander;
    }

    @Override
    public void usage(StringBuilder out, String indent) {
      appendOptions(commander, out, indent);
      appendSubstitutionParamsSection(out, indent);
    }

    private void appendSubstitutionParamsSection(StringBuilder out, String indent) {
      out.append(lineSeparator()).append(indent).append("Allowed substitution parameters:").append(lineSeparator());
      Map<String, String> allParams = ParameterSubstitutor.getAllParams();
      allParams.forEach((param, explanation) -> out.append(indent).append("    ").append(param).append("    ").append(explanation).append(lineSeparator()));
    }

    private void appendOptions(JCommander jCommander, StringBuilder out, String indent) {
      List<ParameterDescription> sorted = jCommander.getParameters();
      sorted.sort(Comparator.comparing(ParameterDescription::getLongestName));
      boolean containsRequiredOption = sorted.stream().anyMatch(pd -> pd.getParameter().required());
      int maxParamLength = sorted.stream().map(pd -> pd.getNames().length()).max(Integer::compareTo).get();
      String requiredHint = " (required)";
      if (containsRequiredOption) {
        maxParamLength += requiredHint.length();
      }

      // Display all the names and descriptions
      if (sorted.size() > 0) {
        for (ParameterDescription pd : sorted) {
          if (pd.getParameter().hidden()) continue;

          WrappedParameter parameter = pd.getParameter();
          out.append(indent).append("    ").append(pd.getNames()).append(parameter.required() ? requiredHint : "");
          out.append(indent).append("    ");

          int spaces = maxParamLength - pd.getNames().length();
          if (parameter.required()) {
            spaces -= requiredHint.length();
          }
          for (int i = 0; i < spaces; i++) {
            out.append(" ");
          }
          out.append(pd.getDescription());
          Optional<Setting> settingOptional = Setting.findSetting(ConsoleParamsUtils.stripDashDash(pd.getLongestName()));
          if (settingOptional.isPresent()) {
            Setting setting = settingOptional.get();
            Optional<String> defaultValue = setting.getDefaultProperty();

            // special handling
            if (setting == NODE_NAME || setting == STRIPE_NAME) {
              defaultValue = Optional.of("<randomly-generated>");
            }

            defaultValue.ifPresent(s -> out.append(". Default: ").append(s));
          }
          out.append(lineSeparator());
        }
      }
    }
  }
}
