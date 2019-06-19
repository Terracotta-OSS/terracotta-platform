/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.manager;

import com.terracottatech.dynamic_config.cli.command.AttachCommand;
import com.terracottatech.dynamic_config.cli.command.DumpTopology;
import com.terracottatech.dynamic_config.cli.command.DetachCommand;
import com.terracottatech.dynamic_config.cli.command.DynamicConfigCommand;
import com.terracottatech.dynamic_config.cli.command.MainCommand;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandManager {
  private final Map<String, DynamicConfigCommand> commandMap;

  public CommandManager() {
    this.commandMap = Stream.of(
        new MainCommand(),
        new AttachCommand(),
        new DetachCommand(),
        new DumpTopology()
    ).collect(
        Collectors.toMap(
            DynamicConfigCommand::getName,
            Function.identity(),
            (u, v) -> {
              throw new AssertionError("Found duplicate command: " + u.getName());
            },
            LinkedHashMap::new
        )
    );
  }

  public Collection<DynamicConfigCommand> getCommands() {
    return Collections.unmodifiableCollection(commandMap.values());
  }

  public DynamicConfigCommand getCommand(String name) {
    if (!commandMap.containsKey(name)) {
      throw new IllegalArgumentException("DynamicConfigCommand not found: " + name);
    }
    return commandMap.get(name);
  }
}
