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
package org.terracotta.config.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigurationModel {

  private String offheapUnit;
  private int clientReconnectWindow = -1;
  private int leaseLength = -1;
  private boolean platformPersistence;
  private boolean backups;

  private final List<String> serverNames = new ArrayList<>();
  private final List<Map.Entry<String, Integer>> offheapResources = new ArrayList<>();
  private final List<String> dataDirectories = new ArrayList<>();

  public ConfigurationModel() {
  }

  public String getOffheapUnit() {
    return offheapUnit;
  }

  public void setOffheapUnit(String offheapUnit) {
    this.offheapUnit = offheapUnit;
  }

  public List<Map.Entry<String, Integer>> getOffheapResources() {
    return offheapResources;
  }

  public List<String> getDataDirectories() {
    return dataDirectories;
  }

  public int getClientReconnectWindow() {
    return clientReconnectWindow;
  }

  public void setClientReconnectWindow(int clientReconnectWindow) {
    this.clientReconnectWindow = clientReconnectWindow;
  }

  public List<String> getServerNames() {
    return serverNames;
  }

  public boolean isPlatformPersistence() {
    return platformPersistence;
  }

  public void setPlatformPersistence(boolean platformPersistence) {
    this.platformPersistence = platformPersistence;
  }

  public int getLeaseLength() {
    return leaseLength;
  }

  public void setLeaseLength(int leaseLength) {
    this.leaseLength = leaseLength;
  }

  public boolean isBackups() {
    return backups;
  }

  public void setBackups(boolean backups) {
    this.backups = backups;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ConfigurationModel that = (ConfigurationModel) o;

    if (clientReconnectWindow != that.clientReconnectWindow) return false;
    if (leaseLength != that.leaseLength) return false;
    if (platformPersistence != that.platformPersistence) return false;
    if (backups != that.backups) return false;
    if (offheapUnit != null ? !offheapUnit.equals(that.offheapUnit) : that.offheapUnit != null) return false;
    if (serverNames != null ? !serverNames.equals(that.serverNames) : that.serverNames != null) return false;
    if (offheapResources != null ? !offheapResources.equals(that.offheapResources) : that.offheapResources != null)
      return false;
    return dataDirectories != null ? dataDirectories.equals(that.dataDirectories) : that.dataDirectories == null;
  }

  @Override
  public int hashCode() {
    int result = offheapUnit != null ? offheapUnit.hashCode() : 0;
    result = 31 * result + clientReconnectWindow;
    result = 31 * result + leaseLength;
    result = 31 * result + (platformPersistence ? 1 : 0);
    result = 31 * result + (backups ? 1 : 0);
    result = 31 * result + (serverNames != null ? serverNames.hashCode() : 0);
    result = 31 * result + (offheapResources != null ? offheapResources.hashCode() : 0);
    result = 31 * result + (dataDirectories != null ? dataDirectories.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ConfigurationModel{" +
        "offheapUnit='" + offheapUnit + '\'' +
        ", clientReconnectWindow=" + clientReconnectWindow +
        ", leaseLength=" + leaseLength +
        ", platformPersistence=" + platformPersistence +
        ", backups=" + backups +
        ", serverNames=" + serverNames +
        ", offheapResources=" + offheapResources +
        ", dataDirectories=" + dataDirectories +
        '}';
  }
}
