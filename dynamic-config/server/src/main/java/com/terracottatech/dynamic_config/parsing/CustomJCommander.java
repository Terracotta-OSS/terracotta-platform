/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.WrappedParameter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

public class CustomJCommander extends JCommander {
  private final String programName;
  private final Collection<String> userSpecifiedOptions = new HashSet<>();

  public CustomJCommander(String programName, Object object) {
    super(object);
    this.programName = programName;

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
    out.append(indent).append("Usage: ").append(programName).append(" [options]");
    out.append("\n");
    appendOptions(this, out, indent);
  }

  private void appendOptions(JCommander jCommander, StringBuilder out, String indent) {
    List<ParameterDescription> sorted = jCommander.getParameters();
    sorted.sort(Comparator.comparing(ParameterDescription::getLongestName));
    int maxParamLength = sorted.stream().map(pd -> pd.getNames().length()).max(Integer::compareTo).get();

    // Display all the names and descriptions
    if (sorted.size() > 0) {
      out.append(indent).append("Options:\n");
      for (ParameterDescription pd : sorted) {
        if (pd.getParameter().hidden()) continue;

        WrappedParameter parameter = pd.getParameter();
        out.append(indent).append("    ").append(pd.getNames()).append(parameter.required() ? " (required)" : "");
        String defaultValue = DefaultSettings.getDefaultValueFor(ConsoleParamsUtils.stripDashDash(pd.getLongestName()));
        if (defaultValue != null) {
          out.append(indent).append("    ");
          for (int i = 0; i < maxParamLength - pd.getNames().length(); i++) {
            out.append(" ");
          }
          out.append("(Default: ").append(defaultValue).append(")");
        }
        out.append("\n");
      }
    }
  }
}
