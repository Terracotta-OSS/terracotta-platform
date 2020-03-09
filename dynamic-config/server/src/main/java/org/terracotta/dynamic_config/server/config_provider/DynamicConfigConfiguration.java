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
package org.terracotta.dynamic_config.server.config_provider;

import com.tc.services.MappedStateCollector;
import com.tc.text.PrettyPrintable;
import org.terracotta.config.Consistency;
import org.terracotta.config.FailoverPriority;
import org.terracotta.config.Property;
import org.terracotta.config.Server;
import org.terracotta.config.Servers;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcConfiguration;
import org.terracotta.config.TcProperties;
import org.terracotta.configuration.Configuration;
import org.terracotta.configuration.ConfigurationException;
import org.terracotta.configuration.FailoverBehavior;
import org.terracotta.configuration.ServerConfiguration;
import org.terracotta.entity.ServiceProviderConfiguration;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

class DynamicConfigConfiguration implements Configuration, PrettyPrintable {
  private final TcConfiguration configuration;
  private final boolean partialConfig;

  DynamicConfigConfiguration(TcConfiguration configuration, boolean partialConfig) {
    this.configuration = configuration;
    this.partialConfig = partialConfig;
  }

  public TcConfig getPlatformConfiguration() {
    return configuration.getPlatformConfiguration();
  }

  @Override
  public boolean isPartialConfiguration() {
    return partialConfig;
  }

  @Override
  public ServerConfiguration getDefaultServerConfiguration(String serverName) throws ConfigurationException {
    Server defaultServer;
    Servers servers = configuration.getPlatformConfiguration().getServers();
    if (serverName != null) {
      defaultServer = findServer(servers, serverName);
    } else {
      defaultServer = getDefaultServer(servers);
    }
    return new ServerConfigurationImpl(defaultServer, servers.getClientReconnectWindow());
  }

  @Override
  public List<ServerConfiguration> getServerConfigurations() {
    Servers servers = configuration.getPlatformConfiguration().getServers();
    int reconnect = servers.getClientReconnectWindow();
    List<Server> list = servers.getServer();
    List<ServerConfiguration> configs = new ArrayList<>(list.size());
    list.forEach(s -> configs.add(new ServerConfigurationImpl(s, reconnect)));
    return configs;
  }

  @Override
  public Properties getTcProperties() {
    TcProperties props = configuration.getPlatformConfiguration().getTcProperties();
    Properties converted = new Properties();
    if (props != null) {
      List<Property> list = props.getProperty();
      list.forEach(p -> converted.setProperty(p.getName().trim(), p.getValue().trim()));
    }
    return converted;
  }

  @Override
  public FailoverBehavior getFailoverPriority() {
    FailoverPriority priority = configuration.getPlatformConfiguration().getFailoverPriority();
    if (priority != null) {
      String available = priority.getAvailability();
      Consistency consistent = priority.getConsistency();
      if (consistent != null) {
        int votes = (consistent.getVoter() != null) ? consistent.getVoter().getCount() : 0;
        return new FailoverBehavior(FailoverBehavior.Type.CONSISTENCY, votes);
      } else {
        if (available == null) {
          return null;
        } else {
          return new FailoverBehavior(FailoverBehavior.Type.AVAILABILITY, 0);
        }
      }
    }
    return null;
  }

  @Override
  public List<ServiceProviderConfiguration> getServiceConfigurations() {
    return this.configuration.getServiceConfigurations();
  }

  @Override
  public <T> List<T> getExtendedConfiguration(Class<T> type) {
    return this.configuration.getExtendedConfiguration(type);
  }

  @Override
  public String getRawConfiguration() {
    return configuration.toString();
  }

  @Override
  public Map<String, ?> getStateMap() {
    MappedStateCollector mappedStateCollector = new MappedStateCollector("collector");
    this.configuration.addStateTo(mappedStateCollector);
    return mappedStateCollector.getMap();
  }

  private Server getDefaultServer(Servers servers) throws ConfigurationException {
    List<Server> serverList = servers.getServer();
    if (serverList.size() == 1) {
      return serverList.get(0);
    }

    try {
      Set<InetAddress> allLocalInetAddresses = getAllLocalInetAddresses();
      Server defaultServer = null;
      for (Server server : serverList) {
        if (allLocalInetAddresses.contains(InetAddress.getByName(server.getHost()))) {
          if (defaultServer == null) {
            defaultServer = server;
          } else {
            throw new ConfigurationException("You have not specified a name for your Terracotta server, and" + " there are "
                + serverList.size() + " servers defined in the Terracotta configuration file. "
                + "The script can not automatically choose between the following server names: "
                + defaultServer.getName() + ", " + server.getName()
                + ". Pass the desired server name to the script using " + "the -n flag.");

          }
        }
      }
      return defaultServer;
    } catch (UnknownHostException uhe) {
      throw new ConfigurationException("Exception when trying to find the default server configuration", uhe);
    }
  }

  private Set<InetAddress> getAllLocalInetAddresses() {
    Set<InetAddress> localAddresses = new HashSet<>();
    Enumeration<NetworkInterface> networkInterfaces;
    try {
      networkInterfaces = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e) {
      throw new RuntimeException(e);
    }
    while (networkInterfaces.hasMoreElements()) {
      Enumeration<InetAddress> inetAddresses = networkInterfaces.nextElement().getInetAddresses();
      while (inetAddresses.hasMoreElements()) {
        localAddresses.add(inetAddresses.nextElement());
      }
    }
    return localAddresses;
  }

  private static Server findServer(Servers servers, String serverName) throws ConfigurationException {
    for (Server server : servers.getServer()) {
      if (server.getName().equals(serverName)) {
        return server;
      }
    }
    throw new ConfigurationException("You have specified server name '" + serverName
        + "' which does not exist in the specified configuration. \n\n"
        + "Please check your settings and try again.");
  }
}
