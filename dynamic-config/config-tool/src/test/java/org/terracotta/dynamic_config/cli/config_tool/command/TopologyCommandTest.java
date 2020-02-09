/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.cli.config_tool.command;

import org.junit.Test;
import org.terracotta.dynamic_config.cli.command.Metadata;
import org.terracotta.dynamic_config.cli.config_tool.BaseTest;

import java.net.InetSocketAddress;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.cli.command.Injector.inject;
import static org.terracotta.dynamic_config.cli.config_tool.converter.OperationType.NODE;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public abstract class TopologyCommandTest<C extends TopologyCommand> extends BaseTest {

  @Test
  public void test_defaults() {
    C command = newCommand();
    assertThat(command.getOperationType(), is(equalTo(NODE)));
    assertThat(command.getSource(), is(nullValue()));
    assertThat(command.getDestination(), is(nullValue()));
    assertThat(Metadata.getName(command), is(equalTo(command.getClass().getSimpleName().toLowerCase().substring(0, 6))));
  }

  @Test
  public void test_validate_failures() {
    assertThat(
        () -> newCommand()
            .setOperationType(NODE)
            .setDestination(InetSocketAddress.createUnresolved("localhost", 9410))
            .setSource(InetSocketAddress.createUnresolved("localhost", 9410))
            .validate(),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("The destination endpoint and the source endpoint must not be the same")))));

    assertThat(
        () -> newCommand()
            .setOperationType(null)
            .setDestination(InetSocketAddress.createUnresolved("localhost", 9410))
            .setSource(InetSocketAddress.createUnresolved("localhost", 9411))
            .validate(),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Missing type")))));

    assertThat(
        () -> newCommand()
            .setDestination(InetSocketAddress.createUnresolved("localhost", 9410))
            .validate(),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Missing source node")))));

    assertThat(
        () -> newCommand()
            .setDestination(null)
            .setSource(InetSocketAddress.createUnresolved("localhost", 9411))
            .validate(),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Missing destination node")))));
  }

  protected final C newCommand() {
    return inject(newTopologyCommand(), diagnosticServiceProvider, multiDiagnosticServiceProvider, nomadManager, restartService);
  }

  protected abstract C newTopologyCommand();
}