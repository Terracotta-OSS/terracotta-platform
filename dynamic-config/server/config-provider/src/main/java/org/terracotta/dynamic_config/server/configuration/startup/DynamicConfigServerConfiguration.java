/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import org.terracotta.common.struct.TimeUnit;
import org.terracotta.configuration.ServerConfiguration;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.server.GroupPortMapper;
import org.terracotta.dynamic_config.api.server.PathResolver;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.function.Supplier;

/**
 * @author Mathieu Carbou
 */
class DynamicConfigServerConfiguration implements ServerConfiguration {

  private final Node node;
  private final Supplier<NodeContext> nodeContextSupplier;
  private final IParameterSubstitutor substitutor;
  private final GroupPortMapper groupPortMapper;
  private final PathResolver pathResolver;
  private final boolean unConfigured;

  public DynamicConfigServerConfiguration(Node node, Supplier<NodeContext> nodeContextSupplier, IParameterSubstitutor substitutor, GroupPortMapper groupPortMapper, PathResolver pathResolver, boolean unConfigured) {
    this.node = node;
    this.nodeContextSupplier = nodeContextSupplier;
    this.substitutor = substitutor;
    this.groupPortMapper = groupPortMapper;
    this.pathResolver = pathResolver;
    this.unConfigured = unConfigured;
  }

  @Override
  public InetSocketAddress getTsaPort() {
    return InetSocketAddress.createUnresolved(substitutor.substitute(node.getBindAddress().orDefault()), node.getPort().orDefault());
  }

  @Override
  public InetSocketAddress getGroupPort() {
    // this call will always load the current nomad version (which can be increased) to find the information
    // on the node we are running into
    Node currentNode = nodeContextSupplier.get().getNode();
    // this.node is the node information that was initially used to build this ServerConfiguration
    // it can be the same node we run into, but it can also be another node of the stripe
    int groupPort = groupPortMapper.getPeerGroupPort(this.node, currentNode);
    return InetSocketAddress.createUnresolved(substitutor.substitute(this.node.getGroupBindAddress().orDefault()), groupPort);
  }

  @Override
  public String getHost() {
    // substitutions not allowed on hostname since hostname-port is a key to identify a node
    // any substitution is allowed but resolved eagerly when parsing the CLI
    return node.getHostname();
  }

  @Override
  public String getName() {
    // substitutions not allowed on name since stripe ID / name is a key to identify a node
    return node.getName();
  }

  @Override
  public int getClientReconnectWindow() {
    return nodeContextSupplier.get().getCluster().getClientReconnectWindow().orDefault().getExactQuantity(TimeUnit.SECONDS).intValueExact();
  }

  @Override
  public File getLogsLocation() {
    if (unConfigured) {
      return null;
    }
    RawPath rawPath = node.getLogDir().orDefault();
    // rawPath can be:
    // - non-null if user has set a value
    // - non-null if user didn't set a value and the default is returned (terracotta.config.logDir.noDefault not set to true)
    // - null     if user didn't set a value and terracotta.config.logDir.noDefault is set to true
    if (rawPath == null) {
      return null;
    }
    String sanitizedNodeName = node.getName().replace(":", "-"); // Sanitize for path
    return substitutor.substitute(pathResolver.resolve(rawPath.toPath().resolve(sanitizedNodeName))).toFile();
  }

  @Override
  public String toString() {
    return node.getName() + "@" + node.getInternalHostPort();
  }
}
