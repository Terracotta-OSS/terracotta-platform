package org.terracotta.management.entity.server;

import org.terracotta.management.entity.ManagementAgent;
import org.terracotta.management.entity.ManagementAgentConfig;
import org.terracotta.voltron.proxy.server.ProxiedServerEntity;

/**
 * @author Mathieu Carbou
 */
public class ManagementAgentServerEntity extends ProxiedServerEntity<ManagementAgent> {

  public ManagementAgentServerEntity(ManagementAgentConfig config) {
    super(ManagementAgent.class, new ManagementAgentImpl(config));
  }
  
}
