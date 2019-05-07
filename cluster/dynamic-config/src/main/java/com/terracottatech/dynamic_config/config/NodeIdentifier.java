/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.config;


public class NodeIdentifier {
  private final String stripeName;
  private final String serverName;

  public NodeIdentifier(String stripeName, String serverName) {
    this.stripeName = stripeName;
    this.serverName = serverName;
  }

  public String getStripeName() {
    return stripeName;
  }

  public String getServerName() {
    return serverName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    NodeIdentifier that = (NodeIdentifier) o;

    if (!stripeName.equals(that.stripeName)) return false;
    return serverName.equals(that.serverName);
  }

  @Override
  public int hashCode() {
    int result = stripeName.hashCode();
    result = 31 * result + serverName.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "NodeIdentifier{" +
        "stripeName='" + stripeName + '\'' +
        ", serverName='" + serverName + '\'' +
        '}';
  }
}
