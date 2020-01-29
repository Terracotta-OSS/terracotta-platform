/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameters;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.cli.command.Usage;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "unset", commandDescription = "Unset configuration properties")
@Usage("unset -s <hostname[:port]> -c <[namespace:]property>,<[namespace:]property>...")
public class UnsetCommand extends ConfigurationMutationCommand {
  public UnsetCommand() {
    super(Operation.UNSET);
  }
}
