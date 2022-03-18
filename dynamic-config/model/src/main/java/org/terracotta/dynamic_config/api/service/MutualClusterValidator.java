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
package org.terracotta.dynamic_config.api.service;

import org.terracotta.dynamic_config.api.model.Cluster;

import java.util.Objects;

import static org.terracotta.dynamic_config.api.model.SettingName.CLIENT_LEASE_DURATION;
import static org.terracotta.dynamic_config.api.model.SettingName.CLIENT_RECONNECT_WINDOW;
import static org.terracotta.dynamic_config.api.model.SettingName.CLUSTER_NAME;
import static org.terracotta.dynamic_config.api.model.SettingName.FAILOVER_PRIORITY;
import static org.terracotta.dynamic_config.api.model.SettingName.OFFHEAP_RESOURCES;
import static org.terracotta.dynamic_config.api.model.SettingName.SECURITY_AUTHC;
import static org.terracotta.dynamic_config.api.model.SettingName.SECURITY_SSL_TLS;
import static org.terracotta.dynamic_config.api.model.SettingName.SECURITY_WHITELIST;

public class MutualClusterValidator {
  private final Cluster major;
  private final Cluster minor;

  public MutualClusterValidator(Cluster major, Cluster minor) {
    this.major = major;
    this.minor = minor;
  }

  public void validate() throws ClusterConfigMismatchException {
    if (!Objects.equals(major.getName(), minor.getName())) {
      fail(CLUSTER_NAME, major.getName(), minor.getName());
    }
    if (!Objects.equals(major.getClientLeaseDuration(), minor.getClientLeaseDuration())) {
      fail(CLIENT_LEASE_DURATION, major.getClientLeaseDuration(), minor.getClientLeaseDuration());
    }
    if (!Objects.equals(major.getClientReconnectWindow(), minor.getClientReconnectWindow())) {
      fail(CLIENT_RECONNECT_WINDOW, major.getClientReconnectWindow(), minor.getClientReconnectWindow());
    }
    if (!Objects.equals(major.getFailoverPriority(), minor.getFailoverPriority())) {
      fail(FAILOVER_PRIORITY, major.getFailoverPriority(), minor.getFailoverPriority());
    }
    if (!Objects.equals(major.getOffheapResources(), minor.getOffheapResources())) {
      fail(OFFHEAP_RESOURCES, major.getOffheapResources(), minor.getOffheapResources());
    }
    if (!Objects.equals(major.getSecurityAuthc(), minor.getSecurityAuthc())) {
      fail(SECURITY_AUTHC, major.getSecurityAuthc(), minor.getSecurityAuthc());
    }
    if (!Objects.equals(major.getSecuritySslTls(), minor.getSecuritySslTls())) {
      fail(SECURITY_SSL_TLS, major.getSecuritySslTls(), minor.getSecuritySslTls());
    }
    if (!Objects.equals(major.getSecurityWhitelist(), minor.getSecurityWhitelist())) {
      fail(SECURITY_WHITELIST, major.getSecurityWhitelist(), minor.getSecurityWhitelist());
    }
  }

  private <T> void fail(String settingName, T majorSetting, T minorSetting) {
    throw new ClusterConfigMismatchException("Mismatch found in " + settingName + " setting between target cluster with " +
        "nodes: " + major.getNodeAddresses() + " and incoming cluster with nodes: " + minor.getNodeAddresses() + ". " +
        "Expected value: " + majorSetting + ", but found: " + minorSetting);
  }
}
