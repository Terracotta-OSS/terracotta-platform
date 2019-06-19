/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnection;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnectionFactory;
import com.terracottatech.dynamic_config.cli.common.InetSocketAddressConverter;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.diagnostic.DynamicConfigService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.utilities.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

@Parameters(commandNames = "dump-topology", commandDescription = "Dump the cluster topology")
@Usage("dump-topology -d HOST[:PORT] -o output.json")
public class DumpTopology extends Command {
  private static final Logger LOGGER = LoggerFactory.getLogger(DumpTopology.class);

  @Parameter(required = true, names = {"-d"}, description = "Destination node", converter = InetSocketAddressConverter.class)
  private InetSocketAddress node;

  @Parameter(names = {"-o"}, description = "Destination node", converter = PathConverter.class)
  private Path output;

  @Resource public MultiDiagnosticServiceConnectionFactory connectionFactory;

  public DumpTopology setNode(InetSocketAddress node) {
    this.node = node;
    return this;
  }

  @Override
  public void validate() {
    if (node == null) {
      throw new IllegalArgumentException("Missing destination node.");
    }
  }

  @Override
  public final void run() {
    try (MultiDiagnosticServiceConnection connections = connectionFactory.createConnection(Collections.singletonList(node))) {
      Cluster topology = connections.getDiagnosticService(node).get().getProxy(DynamicConfigService.class).getTopology();
      if (output == null) {
        LOGGER.info("Topology from '{}':\n{}", node, Json.toPrettyJson(topology));
      } else {
        try {
          Files.write(output, Json.toPrettyJson(topology).getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }
    }
  }
}
