package org.terracotta.management.tms.entity.client;

import com.terracottatech.management.voltron.tms.entity.TmsAgent;
import com.terracottatech.management.voltron.tms.entity.TmsAgentConfig;
import org.terracotta.voltron.proxy.client.ProxyEntityClientService;

/**
 * @author Mathieu Carbou
 */
public class TmsAgentEntityClientService extends ProxyEntityClientService<TmsAgentEntity, TmsAgentConfig> {
  public TmsAgentEntityClientService() {
    super(TmsAgentEntity.class, TmsAgent.class, TmsAgentConfig.class);
  }
}
