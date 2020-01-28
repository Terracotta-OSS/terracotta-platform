/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.WrappedParameter;
import com.beust.jcommander.internal.Lists;
import com.terracottatech.dynamic_config.cli.service.command.Command;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static java.lang.System.lineSeparator;
import static java.util.function.Predicate.isEqual;

/**
 * Class containing overridden usage methods from JCommander.
 */
public class CustomJCommander extends JCommander {
  private final CommandRepository commandRepository;
  private String toolName;

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

  @Override
  public void usage(String commandName, StringBuilder out, String indent) {
    String description = getCommandDescription(commandName);
    JCommander jc = getCommands().get(commandName);
    if (description != null) {
      out.append(indent).append(description).append(lineSeparator());
    }
    appendUsage(commandRepository.getCommand(commandName), out, indent);
    appendOptions(jc, out, indent);
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

    // Show Commands
    if (hasCommands) {
      out.append(lineSeparator()).append(lineSeparator()).append("Commands:").append(lineSeparator());
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

  @Override
  public Map<String, JCommander> getCommands() {
    // force an ordering of commands by name
    return new TreeMap<>(super.getCommands());
  }

  private void appendUsage(Command command, StringBuilder out, String indent) {
    out.append(indent).append("Usage:").append(lineSeparator());
    out.append(indent).append("    ").append(Metadata.getUsage(command).replace(lineSeparator(), lineSeparator() + "    " + indent)).append(lineSeparator());
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
}
