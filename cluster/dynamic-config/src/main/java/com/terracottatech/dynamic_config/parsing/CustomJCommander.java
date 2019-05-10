/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.parsing;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.WrappedParameter;
import com.beust.jcommander.internal.Lists;
import com.terracottatech.dynamic_config.config.DefaultSettings;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.terracottatech.dynamic_config.util.ConsoleParamsUtils.stripDashDash;

public class CustomJCommander extends JCommander {
  private final String programName;
  private final Set<String> userSpecifiedOptions = new HashSet<>();

  public CustomJCommander(String programName, Object object) {
    super(object);
    this.programName = programName;

    // Hacky way to get the specified options, since JCommander doesn't provide this functionality out-of-the-box
    addConverterInstanceFactory((parameter, forType) -> {
      userSpecifiedOptions.add(parameter.names()[0]);
      return null;
    });
  }

  public Set<String> getUserSpecifiedOptions() {
    return Collections.unmodifiableSet(userSpecifiedOptions);
  }

  @Override
  public void usage(StringBuilder out, String indent) {
    out.append(indent).append("Usage: ").append(programName).append(" [options]");

    if (getMainParameter() != null) {
      out.append(" ").append(getMainParameter().getDescription());
    }
    out.append("\n");
    appendOptions(this, out, indent);
  }

  private void appendOptions(JCommander jCommander, StringBuilder out, String indent) {
    // Align the descriptions at the "longestName" column
    int longestName = 0;
    List<ParameterDescription> sorted = Lists.newArrayList();
    for (ParameterDescription pd : jCommander.getParameters()) {
      if (!pd.getParameter().hidden()) {
        sorted.add(pd);
        int length = pd.getNames().length() + 2;
        if (length > longestName) {
          longestName = length;
        }
      }
    }

    sorted.sort(Comparator.comparing(ParameterDescription::getLongestName));
    int maxParamLength = sorted.stream().map(pd -> pd.getLongestName().length()).max(Integer::compareTo).get();

    // Display all the names and descriptions
    if (sorted.size() > 0) {
      out.append(indent).append("Options:\n");
    }

    for (ParameterDescription pd : sorted) {
      WrappedParameter parameter = pd.getParameter();
      out.append(indent).append("    ").append(pd.getNames()).append(parameter.required() ? " (required)" : "");
      String defaultValue = DefaultSettings.getDefaultValueFor(stripDashDash(pd.getLongestName()));
      if (defaultValue != null) {
        out.append(indent).append("    ");
        for (int i = 0; i < maxParamLength - pd.getLongestName().length(); i++) {
          out.append(" ");
        }
        out.append("(Default: ").append(defaultValue).append(")");
      }
      out.append("\n");
    }
  }
}
