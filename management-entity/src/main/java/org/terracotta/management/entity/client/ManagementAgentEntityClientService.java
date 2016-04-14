package org.terracotta.management.entity.client;

import org.terracotta.management.entity.ManagementAgent;
import org.terracotta.management.entity.ManagementAgentConfig;
import org.terracotta.voltron.proxy.client.ProxyEntityClientService;

/**
 * @author Mathieu Carbou
 */
public class ManagementAgentEntityClientService extends ProxyEntityClientService<ManagementAgentEntity, ManagementAgentConfig> {

  public ManagementAgentEntityClientService() {
    super(ManagementAgentEntity.class, ManagementAgent.class);
  }

  @Override
  public byte[] serializeConfiguration(ManagementAgentConfig configuration) {
    return configuration.serialize();
  }

  @Override
  public ManagementAgentConfig deserializeConfiguration(byte[] configuration) {
    return ManagementAgentConfig.deserialize(configuration);
  }

}
