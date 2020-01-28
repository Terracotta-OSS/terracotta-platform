/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.config_tool.command;

import com.terracottatech.dynamic_config.cli.command.Metadata;
import com.terracottatech.dynamic_config.cli.config_tool.BaseTest;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Collections;

import static com.terracottatech.dynamic_config.cli.command.Injector.inject;
import static com.terracottatech.dynamic_config.cli.config_tool.converter.AttachmentType.NODE;
import static com.terracottatech.testing.ExceptionMatcher.throwing;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public abstract class TopologyCommandTest<C extends TopologyCommand> extends BaseTest {

  @Test
  public void test_defaults() {
    C command = newCommand();
    assertThat(command.getAttachmentType(), is(equalTo(NODE)));
    assertThat(command.getSources(), is(equalTo(Collections.emptyList())));
    assertThat(command.getDestination(), is(nullValue()));
    assertThat(Metadata.getName(command), is(equalTo(command.getClass().getSimpleName().toLowerCase().substring(0, 6))));
  }

  @Test
  public void test_validate_failures() {
    assertThat(
        () -> newCommand()
            .setAttachmentType(NODE)
            .setDestination(InetSocketAddress.createUnresolved("localhost", 9410))
            .setSources(InetSocketAddress.createUnresolved("localhost", 9410), InetSocketAddress.createUnresolved("localhost", 9411))
            .validate(),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("The destination endpoint must not be listed in the source endpoints.")))));

    assertThat(
        () -> newCommand()
            .setAttachmentType(null)
            .setDestination(InetSocketAddress.createUnresolved("localhost", 9410))
            .setSources(InetSocketAddress.createUnresolved("localhost", 9410), InetSocketAddress.createUnresolved("localhost", 9411))
            .validate(),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Missing type.")))));

    assertThat(
        () -> newCommand()
            .setDestination(InetSocketAddress.createUnresolved("localhost", 9410))
            .validate(),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Missing source nodes.")))));

    assertThat(
        () -> newCommand()
            .setDestination(null)
            .setSources(InetSocketAddress.createUnresolved("localhost", 9411))
            .validate(),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Missing destination node.")))));
  }

  @Test
  public void test_validate() {
    newCommand()
        .setAttachmentType(NODE)
        .setDestination(InetSocketAddress.createUnresolved("localhost", 9410))
        .setSources(InetSocketAddress.createUnresolved("localhost", 9411))
        .validate();
  }

  protected final C newCommand() {
    return inject(newTopologyCommand(), diagnosticServiceProvider, multiDiagnosticServiceProvider, nomadManager, restartService);
  }

  protected abstract C newTopologyCommand();
}