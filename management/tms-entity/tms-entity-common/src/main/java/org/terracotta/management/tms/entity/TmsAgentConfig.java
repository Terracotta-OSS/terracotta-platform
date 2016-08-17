package org.terracotta.management.tms.entity;

import org.terracotta.management.model.Objects;

import java.io.Serializable;

/**
 * @author Mathieu Carbou
 */
public final class TmsAgentConfig implements Serializable {

  private static final long serialVersionUID = 1;

  // name must be hardcoded because it reference a class name in client package and is used on server-side
  public static final String ENTITY_TYPE = "org.terracotta.management.tms.entity.client.TmsAgentEntity";

  private final String connectionName;
  private final String stripeName;

  public TmsAgentConfig(String connectionName, String stripeName) {
    this.connectionName = Objects.requireNonNull(connectionName);
    this.stripeName = Objects.requireNonNull(stripeName);
  }

  public TmsAgentConfig(String connectionName) {
    this.connectionName = Objects.requireNonNull(connectionName);
    this.stripeName = null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TmsAgentConfig that = (TmsAgentConfig) o;

    if (!connectionName.equals(that.connectionName)) return false;
    return stripeName != null ? stripeName.equals(that.stripeName) : that.stripeName == null;

  }

  @Override
  public int hashCode() {
    int result = connectionName.hashCode();
    result = 31 * result + (stripeName != null ? stripeName.hashCode() : 0);
    return result;
  }

  public String getConnectionName() {
    return connectionName;
  }

  public String getStripeName() {
    return stripeName;
  }

  public TmsAgentConfig withStripeName(String stripeName) {
    return new TmsAgentConfig(connectionName, stripeName);
  }

}
