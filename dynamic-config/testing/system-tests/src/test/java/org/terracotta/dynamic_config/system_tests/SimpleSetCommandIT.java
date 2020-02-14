/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.io.File.separator;
import static org.hamcrest.Matchers.containsString;

@ClusterDefinition
public class SimpleSetCommandIT extends DynamicConfigIT {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  /*<--Single Node Tests-->*/
  @Test
  public void setOffheapResource() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=512MB");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main");
    waitUntil(out::getLog, containsString("offheap-resources.main=512MB"));
  }

  @Test
  public void setTcProperties() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.something=value");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.something");
    waitUntil(out::getLog, containsString("stripe.1.node.1.tc-properties.something=value"));
  }

  @Test
  public void setClientReconnectWindow() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window=10s");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window");
    waitUntil(out::getLog, containsString("client-reconnect-window=10s"));
  }

  @Test
  public void setSecurityAuthc() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "security-dir=/path/to/security/dir", "-c", "security-authc=file");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "security-authc");
    waitUntil(out::getLog, containsString("security-authc=file"));
  }

  @Test
  public void setNodeGroupPort() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-group-port=9630");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-group-port");
    waitUntil(out::getLog, containsString("stripe.1.node.1.node-group-port=9630"));
  }

  @Test
  public void setSecurityWhitelist() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "security-dir=/path/to/security/dir", "-c", "security-whitelist=true");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "security-whitelist");
    waitUntil(out::getLog, containsString("security-whitelist=true"));
  }

  @Test
  public void setDataDir() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.data-dirs.main=user-data/main/stripe1-node1-data-dir");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.data-dirs.main");
    waitUntil(out::getLog, containsString("stripe.1.node.1.data-dirs.main=user-data" + separator + "main" + separator + "stripe1-node1-data-dir"));
  }

  @Test
  public void setNodeBackupDir() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-backup-dir=backup/stripe1-node1-backup");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-backup-dir");
    waitUntil(out::getLog, containsString("stripe.1.node.1.node-backup-dir=backup" + separator + "stripe1-node1-backup"));
  }

  @Test
  public void setTwoProperties() {
    configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1GB", "-c", "stripe.1.node.1.data-dirs.main=stripe1-node1-data-dir");
    assertCommandSuccessful();

    configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main", "-c", "stripe.1.node.1.data-dirs.main");
    waitUntil(out::getLog, containsString("offheap-resources.main=1GB"));
    waitUntil(out::getLog, containsString("stripe.1.node.1.data-dirs.main=stripe1-node1-data-dir"));
  }
}
