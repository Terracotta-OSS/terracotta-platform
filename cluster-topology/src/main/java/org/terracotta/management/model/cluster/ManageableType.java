/*
 * Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.terracotta.management.model.cluster;

/**
 * @author Mathieu Carbou
 */
public enum ManageableType {

  CACHE_MANAGER_SERVER_ENTITY("org.ehcache.clustered.client.internal.EhcacheClientEntity"),
  TMS_AGENT_SERVER_ENTITY("com.terracottatech.management.voltron.tms.entity.client.TmsAgentEntity"),
  MANAGEMENT_AGENT_SERVER_ENTITY("org.terracotta.management.entity.client.ManagementAgentEntity"),
  COORDINATION_ENTITY("org.terracotta.consensus.entity.client.CoordinationClientEntity"),
  DATASET_SERVER_ENTITY("TBD");

  private final String typeName;

  ManageableType(String typeName) {
    this.typeName = typeName;
  }

  public String getTypeName() {
    return typeName;
  }

  public static ManageableType fromTypeName(String type) {
    for (ManageableType manageableType : values()) {
      if (manageableType.getTypeName().equals(type)) {
        return manageableType;
      }
    }
    return null;
  }

}
