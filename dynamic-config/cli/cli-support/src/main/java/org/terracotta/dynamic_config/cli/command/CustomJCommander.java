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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.WrappedParameter;
import com.beust.jcommander.internal.Lists;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static java.lang.System.in;
import static java.lang.System.lineSeparator;
import static java.util.function.Predicate.isEqual;

/**
 * Class containing overridden usage methods from JCommander.
 */
public class CustomJCommander extends JCommander {
  private final CommandRepository commandRepository;
  private final String toolName;
  private boolean showDeprecated;

  public CustomJCommander(String toolName, CommandRepository commandRepository, Command command) {
    super(command);
    this.toolName = toolName;
    this.commandRepository = commandRepository;
    commandRepository.getCommands()
        .stream()
        .filter(isEqual(command).negate())
        .forEach(cmd -> addCommand(Metadata.getName(cmd), cmd));
  }

  public Optional<Command> getAskedCommand() {
    return Optional.ofNullable(getParsedCommand()).map(commandRepository::getCommand);
  }

  public void printUsage() {
    if (getParsedCommand() != null) {
      usage(getParsedCommand());
    } else {
      usage();
    }
  }

  public void printDeprecatedUsage() {
    showDeprecated = true;
    printUsage();
    showDeprecated = false;
  }

  @Override
  public void usage(String commandName, StringBuilder out, String indent) {
    String description = getCommandDescription(commandName);
    JCommander jc = getCommands().get(commandName);
    if (description != null) {
      out.append(indent).append(description).append(lineSeparator());
    }
    appendUsage(commandRepository.getCommand(commandName), out, indent);
    if (showDeprecated) {
      appendDeprecatedOptions(jc, commandName, out, indent);
    } else {
      appendOptions(jc, out, indent);
    }
  }

  @Override
  public void usage(StringBuilder out, String indent) {
    Map<String, JCommander> commands = getCommands();
    boolean hasCommands = !commands.isEmpty();
    out.append(indent).append("Usage: ").append(toolName).append(" [options]");
    if (hasCommands) {
      out.append(indent).append(" [command] [command-options]");
    }

    out.append(lineSeparator());
    appendOptions(this, out, indent);
    appendDefinitions(out, indent);
    appendCommands(out, indent, commands, hasCommands);
  }

  @Override
  public Map<String, JCommander> getCommands() {
    // force an ordering of commands by name
    return new TreeMap<>(super.getCommands());
  }

  protected void appendDefinitions(StringBuilder out, String indent) {

  }

  private void appendUsage(Command command, StringBuilder out, String indent) {
    out.append(indent).append("Usage:").append(lineSeparator());
    String usage = showDeprecated ? Metadata.getDeprecatedUsage(command) : Metadata.getUsage(command);
    out.append(indent).append("    ").append(usage.replace(lineSeparator(), lineSeparator() + "    " + indent)).append(lineSeparator());
  }

  private void appendDeprecatedOptions(JCommander jCommander, String commandName, StringBuilder out, String indent) {
    int longestName = 0;
    List<DeprecatedParameter> sorted = Lists.newArrayList();
    for (ParameterDescription pd : jCommander.getParameters()) {
      DeprecatedParameter deprecatedParameter = getObject(commandName, pd.getParameterAnnotation());
      if (deprecatedParameter != null) {
        sorted.add(deprecatedParameter);
        int length = String.join(", ", deprecatedParameter.names()).length() + 2;
        if (length > longestName) {
          longestName = length;
        }
      }
    }

    // Sort the options
    sorted.sort(Comparator.comparing(dp -> Arrays.stream(dp.names())
        .sorted()
        .skip(dp.names().length - 1).findAny()
        .get()
        .length()));

    // Display all the names and descriptions
    if (sorted.size() > 0) {
      out.append(indent).append("Deprecated options:").append(lineSeparator());
    }

    for (DeprecatedParameter pd : sorted) {
      out.append(indent).append("    ").append(String.join(", ", pd.names())).append(pd.required() ? " (required)" : "").append(lineSeparator());
      out.append(indent).append("        ").append(pd.description());
      out.append(lineSeparator());
    }
  }

  private DeprecatedParameter getObject(String commandName, Parameter parameter) {
    for (Field f : commandRepository.getCommand(commandName).getClass().getDeclaredFields()) {
      if (parameter.equals(f.getAnnotation(Parameter.class))) {
        return f.getAnnotation(DeprecatedParameter.class);
      }
    }
    return null;
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

    // Sort the options
    sorted.sort(Comparator.comparing(ParameterDescription::getLongestName));

    // Display all the names and descriptions
    if (sorted.size() > 0) {
      out.append(indent).append("Options:").append(lineSeparator());
    }

    for (ParameterDescription pd : sorted) {
      WrappedParameter parameter = pd.getParameter();
      out.append(indent).append("    ").append(pd.getNames()).append(parameter.required() ? " (required)" : "").append(lineSeparator());
      out.append(indent).append("        ").append(pd.getDescription());
      out.append(lineSeparator());
    }
  }

  private void appendCommands(StringBuilder out, String indent, Map<String, JCommander> commands, boolean hasCommands) {
    if (hasCommands) {
      out.append(lineSeparator()).append("Commands:").append(lineSeparator());
      for (Map.Entry<String, JCommander> command : commands.entrySet()) {
        Object arg = command.getValue().getObjects().get(0);
        Parameters p = arg.getClass().getAnnotation(Parameters.class);
        String name = command.getKey();
        if (p == null || !p.hidden()) {
          String description = getCommandDescription(name);
          out.append(indent).append("    ").append(name).append("      ").append(description).append(lineSeparator());
          appendUsage(commandRepository.getCommand(name), out, indent + "    ");

          // Options for this command
          JCommander jc = command.getValue();
          appendOptions(jc, out, "    ");
          out.append(lineSeparator());
        }
      }
    }
  }
}
