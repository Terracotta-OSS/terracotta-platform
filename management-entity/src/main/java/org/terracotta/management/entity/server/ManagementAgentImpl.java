package org.terracotta.management.entity.server;

import org.terracotta.management.entity.ManagementAgent;
import org.terracotta.management.entity.ManagementAgentConfig;

/**
 * @author Mathieu Carbou
 */
public class ManagementAgentImpl implements ManagementAgent {

  private final ManagementAgentConfig config;

  public ManagementAgentImpl(ManagementAgentConfig config) {
    this.config = config;
  }

  //TODO: MATHIEU - implement ManagementAgent API

}
