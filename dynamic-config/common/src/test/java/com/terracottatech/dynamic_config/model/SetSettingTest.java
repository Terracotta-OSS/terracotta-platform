/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.terracottatech.utilities.Measure;
import org.junit.Test;

import java.nio.file.Paths;

import static com.terracottatech.dynamic_config.model.Setting.DATA_DIRS;
import static com.terracottatech.dynamic_config.model.Setting.LICENSE_FILE;
import static com.terracottatech.dynamic_config.model.Setting.NODE_BACKUP_DIR;
import static com.terracottatech.dynamic_config.model.Setting.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_AUDIT_LOG_DIR;
import static com.terracottatech.dynamic_config.model.Setting.SECURITY_DIR;
import static com.terracottatech.dynamic_config.model.Setting.TC_PROPERTIES;
import static com.terracottatech.utilities.MemoryUnit.GB;
import static com.terracottatech.utilities.MemoryUnit.MB;
import static com.terracottatech.utilities.hamcrest.ExceptionMatcher.throwing;
import static java.io.File.separator;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * only test the necessary setters having some logic
 *
 * @author Mathieu Carbou
 */
public class SetSettingTest {

  @Test
  public void test_setProperty_NODE_BACKUP_DIR() {
    Node node = Node.newDefaultNode("localhost");
    NODE_BACKUP_DIR.setProperty(node, ".");
    assertThat(node.getNodeBackupDir(), is(equalTo(Paths.get("."))));
    NODE_BACKUP_DIR.setProperty(node, null);
    assertNull(node.getNodeBackupDir());
  }

  @Test
  public void test_setProperty_SECURITY_DIR() {
    Node node = Node.newDefaultNode("localhost");
    SECURITY_DIR.setProperty(node, ".");
    assertThat(node.getSecurityDir(), is(equalTo(Paths.get("."))));
    SECURITY_DIR.setProperty(node, null);
    assertNull(node.getSecurityDir());
  }

  @Test
  public void test_setProperty_SECURITY_AUDIT_LOG_DIR() {
    Node node = Node.newDefaultNode("localhost");
    SECURITY_AUDIT_LOG_DIR.setProperty(node, ".");
    assertThat(node.getSecurityAuditLogDir(), is(equalTo(Paths.get("."))));
    SECURITY_AUDIT_LOG_DIR.setProperty(node, null);
    assertNull(node.getSecurityAuditLogDir());
  }

  @Test
  public void test_setProperty_LICENSE_FILE() {
    Node node = Node.newDefaultNode("localhost");

    // exception comes from validate call
    assertThat(
        () -> LICENSE_FILE.setProperty(node, null),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("license-file cannot be null")))));

    assertThat(
        () -> LICENSE_FILE.setProperty(node, "a.xml"),
        is(throwing(instanceOf(UnsupportedOperationException.class))));
  }

  @Test
  public void test_setProperty_OFFHEAP_RESOURCES() {
    Node node = Node.newDefaultNode("localhost");
    assertThat(node.getOffheapResources().size(), is(equalTo(1)));
    assertThat(node.getOffheapResources().get("main"), is(equalTo(Measure.of(512, MB))));

    OFFHEAP_RESOURCES.setProperty(node, null);
    assertThat(node.getOffheapResources().size(), is(equalTo(0)));

    node = Node.newDefaultNode("localhost");
    OFFHEAP_RESOURCES.setProperty(node, null, null);
    assertThat(node.getOffheapResources().size(), is(equalTo(0)));

    node = Node.newDefaultNode("localhost");
    OFFHEAP_RESOURCES.setProperty(node, "main", null);
    assertThat(node.getOffheapResources().size(), is(equalTo(0)));

    node = Node.newDefaultNode("localhost");
    OFFHEAP_RESOURCES.setProperty(node, "main", "1GB");
    assertThat(node.getOffheapResources().size(), is(equalTo(1)));
    assertThat(node.getOffheapResources().get("main"), is(equalTo(Measure.of(1, GB))));

    node = Node.newDefaultNode("localhost");
    OFFHEAP_RESOURCES.setProperty(node, null, "main:1GB");
    assertThat(node.getOffheapResources().size(), is(equalTo(1)));
    assertThat(node.getOffheapResources().get("main"), is(equalTo(Measure.of(1, GB))));

    node = Node.newDefaultNode("localhost");
    OFFHEAP_RESOURCES.setProperty(node, null, "main:1GB,second:2GB");
    assertThat(node.getOffheapResources().size(), is(equalTo(2)));
    assertThat(node.getOffheapResources().get("main"), is(equalTo(Measure.of(1, GB))));
    assertThat(node.getOffheapResources().get("second"), is(equalTo(Measure.of(2, GB))));
  }

  @Test
  public void test_setProperty_DATA_DIRS() {
    Node node = Node.newDefaultNode("localhost");
    assertThat(node.getDataDirs().size(), is(equalTo(1)));
    assertThat(node.getDataDirs().get("main"), is(equalTo(Paths.get("%H" + separator + "terracotta" + separator + "user-data" + separator + "main"))));

    DATA_DIRS.setProperty(node, null);
    assertThat(node.getDataDirs().size(), is(equalTo(0)));

    node = Node.newDefaultNode("localhost");
    DATA_DIRS.setProperty(node, null, null);
    assertThat(node.getDataDirs().size(), is(equalTo(0)));

    node = Node.newDefaultNode("localhost");
    DATA_DIRS.setProperty(node, "main", null);
    assertThat(node.getDataDirs().size(), is(equalTo(0)));

    node = Node.newDefaultNode("localhost");
    DATA_DIRS.setProperty(node, "main", "foo/bar");
    assertThat(node.getDataDirs().size(), is(equalTo(1)));
    assertThat(node.getDataDirs().get("main"), is(equalTo(Paths.get("foo/bar"))));

    node = Node.newDefaultNode("localhost");
    DATA_DIRS.setProperty(node, null, "main:foo/bar");
    assertThat(node.getDataDirs().size(), is(equalTo(1)));
    assertThat(node.getDataDirs().get("main"), is(equalTo(Paths.get("foo/bar"))));

    node = Node.newDefaultNode("localhost");
    DATA_DIRS.setProperty(node, null, "main:foo/bar,second:foo/baz");
    assertThat(node.getDataDirs().size(), is(equalTo(2)));
    assertThat(node.getDataDirs().get("main"), is(equalTo(Paths.get("foo/bar"))));
    assertThat(node.getDataDirs().get("second"), is(equalTo(Paths.get("foo/baz"))));
  }

  @Test
  public void test_setProperty_TC_PROPERTIES() {
    Node node = Node.newDefaultNode("localhost").setTcProperty("foo", "bar");
    assertThat(node.getTcProperties().size(), is(equalTo(1)));

    TC_PROPERTIES.setProperty(node, null);
    assertThat(node.getTcProperties().size(), is(equalTo(0)));

    node = Node.newDefaultNode("localhost").setTcProperty("foo", "bar");
    TC_PROPERTIES.setProperty(node, null, null);
    assertThat(node.getTcProperties().size(), is(equalTo(0)));

    node = Node.newDefaultNode("localhost").setTcProperty("main", "bar");
    TC_PROPERTIES.setProperty(node, "main", null);
    assertThat(node.getTcProperties().size(), is(equalTo(0)));

    node = Node.newDefaultNode("localhost").setTcProperty("main", "bar");
    TC_PROPERTIES.setProperty(node, "main", "baz");
    assertThat(node.getTcProperties().size(), is(equalTo(1)));
    assertThat(node.getTcProperties().get("main"), is(equalTo("baz")));

    node = Node.newDefaultNode("localhost").setTcProperty("main", "bar");
    TC_PROPERTIES.setProperty(node, null, "main:baz");
    assertThat(node.getTcProperties().size(), is(equalTo(1)));
    assertThat(node.getTcProperties().get("main"), is(equalTo("baz")));

    node = Node.newDefaultNode("localhost").setTcProperty("main", "bar");
    TC_PROPERTIES.setProperty(node, null, "main:baz1,second:baz1");
    assertThat(node.getTcProperties().size(), is(equalTo(2)));
    assertThat(node.getTcProperties().get("main"), is(equalTo("baz1")));
    assertThat(node.getTcProperties().get("second"), is(equalTo("baz1")));
  }
}