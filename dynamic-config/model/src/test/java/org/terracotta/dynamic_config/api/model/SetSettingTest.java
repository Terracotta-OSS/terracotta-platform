/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.api.model;

import org.junit.Test;
import org.terracotta.common.struct.Measure;

import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.terracotta.common.struct.MemoryUnit.GB;
import static org.terracotta.common.struct.MemoryUnit.MB;
import static org.terracotta.dynamic_config.api.model.Setting.DATA_DIRS;
import static org.terracotta.dynamic_config.api.model.Setting.LICENSE_FILE;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_BACKUP_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_LOGGER_OVERRIDES;
import static org.terracotta.dynamic_config.api.model.Setting.OFFHEAP_RESOURCES;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_AUDIT_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_LOG_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.SECURITY_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.TC_PROPERTIES;

/**
 * only test the necessary setters having some logic
 *
 * @author Mathieu Carbou
 */
public class SetSettingTest {

  @Test
  public void test_setProperty_NODE_BACKUP_DIR() {
    Node node = Testing.newTestNode("node1", "localhost");
    assertFalse(node.getSecurityDir().isConfigured());
    NODE_BACKUP_DIR.setProperty(node, ".");
    assertTrue(node.getBackupDir().isConfigured());
    assertThat(node.getBackupDir().get(), is(equalTo(RawPath.valueOf("."))));
    NODE_BACKUP_DIR.setProperty(node, null);
    assertNull(node.getBackupDir().orDefault());
    assertFalse(node.getBackupDir().isConfigured());
  }

  @Test
  public void test_setProperty_SECURITY_DIR() {
    Node node = Testing.newTestNode("node1", "localhost");
    assertFalse(node.getSecurityDir().isConfigured());
    SECURITY_DIR.setProperty(node, ".");
    assertTrue(node.getSecurityDir().isConfigured());
    assertThat(node.getSecurityDir().get(), is(equalTo(RawPath.valueOf("."))));
    SECURITY_DIR.setProperty(node, null);
    assertNull(node.getSecurityDir().orDefault());
    assertFalse(node.getSecurityDir().isConfigured());
  }

  @Test
  public void test_setProperty_SECURITY_AUDIT_LOG_DIR() {
    Node node = Testing.newTestNode("node1", "localhost");
    assertFalse(node.getSecurityAuditLogDir().isConfigured());
    SECURITY_AUDIT_LOG_DIR.setProperty(node, ".");
    assertTrue(node.getSecurityAuditLogDir().isConfigured());
    assertThat(node.getSecurityAuditLogDir().get(), is(equalTo(RawPath.valueOf("."))));
    SECURITY_AUDIT_LOG_DIR.setProperty(node, null);
    assertNull(node.getSecurityAuditLogDir().orDefault());
    assertFalse(node.getSecurityAuditLogDir().isConfigured());
  }

  @Test
  public void test_setProperty_SECURITY_LOG_DIR() {
    Node node = Testing.newTestNode("node1", "localhost");
    assertFalse(node.getSecurityLogDir().isConfigured());
    SECURITY_LOG_DIR.setProperty(node, ".");
    assertTrue(node.getSecurityLogDir().isConfigured());
    assertThat(node.getSecurityLogDir().get(), is(equalTo(RawPath.valueOf("."))));
    SECURITY_LOG_DIR.setProperty(node, null);
    assertNull(node.getSecurityLogDir().orDefault());
    assertFalse(node.getSecurityLogDir().isConfigured());
  }

  @Test
  public void test_setProperty_LICENSE_FILE() {
    Node node = Testing.newTestNode("node1", "localhost");

    // not throwing - noop (license uninstall)
    LICENSE_FILE.setProperty(node, null);

    LICENSE_FILE.setProperty(node, "a.xml");
  }

  @Test
  public void test_setProperty_OFFHEAP_RESOURCES() {
    Cluster cluster = Testing.newTestCluster();
    assertFalse(cluster.getOffheapResources().isConfigured());
    assertThat(cluster.getOffheapResources().orDefault().size(), is(equalTo(1)));
    assertThat(cluster.getOffheapResources().orDefault().get("main"), is(equalTo(Measure.of(512, MB))));

    OFFHEAP_RESOURCES.setProperty(cluster, null);
    assertTrue(cluster.getOffheapResources().isConfigured());
    assertThat(cluster.getOffheapResources().orDefault().size(), is(equalTo(0)));

    cluster = Testing.newTestCluster();
    OFFHEAP_RESOURCES.setProperty(cluster, "");
    assertTrue(cluster.getOffheapResources().isConfigured());
    assertThat(cluster.getOffheapResources().get().size(), is(equalTo(0)));

    cluster = Testing.newTestCluster();
    OFFHEAP_RESOURCES.setProperty(cluster, null, null);
    assertTrue(cluster.getOffheapResources().isConfigured());
    assertThat(cluster.getOffheapResources().orDefault().size(), is(equalTo(0)));

    cluster = Testing.newTestCluster();
    OFFHEAP_RESOURCES.setProperty(cluster, null, "");
    assertTrue(cluster.getOffheapResources().isConfigured());
    assertThat(cluster.getOffheapResources().get().size(), is(equalTo(0)));

    cluster = Testing.newTestCluster();
    OFFHEAP_RESOURCES.setProperty(cluster, "main", null);
    assertTrue(cluster.getOffheapResources().isConfigured());
    assertThat(cluster.getOffheapResources().get().size(), is(equalTo(0)));

    cluster = Testing.newTestCluster();
    OFFHEAP_RESOURCES.setProperty(cluster, "main", "1GB");
    assertTrue(cluster.getOffheapResources().isConfigured());
    assertThat(cluster.getOffheapResources().get().size(), is(equalTo(1)));
    assertThat(cluster.getOffheapResources().get().get("main"), is(equalTo(Measure.of(1, GB))));

    cluster = Testing.newTestCluster();
    OFFHEAP_RESOURCES.setProperty(cluster, null, "main:1GB");
    assertTrue(cluster.getOffheapResources().isConfigured());
    assertThat(cluster.getOffheapResources().get().size(), is(equalTo(1)));
    assertThat(cluster.getOffheapResources().get().get("main"), is(equalTo(Measure.of(1, GB))));

    cluster = Testing.newTestCluster();
    OFFHEAP_RESOURCES.setProperty(cluster, null, "main:1GB,second:2GB");
    assertTrue(cluster.getOffheapResources().isConfigured());
    assertThat(cluster.getOffheapResources().get().size(), is(equalTo(2)));
    assertThat(cluster.getOffheapResources().get().get("main"), is(equalTo(Measure.of(1, GB))));
    assertThat(cluster.getOffheapResources().get().get("second"), is(equalTo(Measure.of(2, GB))));
  }

  @Test
  public void test_setProperty_DATA_DIRS() {
    Node node = Testing.newTestNode("node1", "localhost");
    assertThat(node.getDataDirs().orDefault().size(), is(equalTo(1)));
    assertThat(node.getDataDirs().orDefault().get("main"), is(equalTo(RawPath.valueOf(Paths.get("%H", "terracotta", "user-data", "main").toString()))));

    DATA_DIRS.setProperty(node, null);
    assertTrue(node.getDataDirs().isConfigured());
    assertThat(node.getDataDirs().orDefault().size(), is(equalTo(0)));

    node = Testing.newTestNode("node1", "localhost");
    DATA_DIRS.setProperty(node, null, ""); // ask for a reset
    assertTrue(node.getDataDirs().isConfigured());
    assertThat(node.getDataDirs().get().size(), is(equalTo(0)));

    node = Testing.newTestNode("node1", "localhost");
    DATA_DIRS.setProperty(node, null, null);
    assertThat(node.getDataDirs().orDefault().size(), is(equalTo(0)));

    node = Testing.newTestNode("node1", "localhost");
    DATA_DIRS.setProperty(node, null, "");
    assertThat(node.getDataDirs().orDefault().size(), is(equalTo(0)));

    node = Testing.newTestNode("node1", "localhost");
    DATA_DIRS.setProperty(node, "main", null);
    assertThat(node.getDataDirs().orDefault().size(), is(equalTo(0)));

    node = Testing.newTestNode("node1", "localhost");
    DATA_DIRS.setProperty(node, "main", "foo/bar");
    assertThat(node.getDataDirs().orDefault().size(), is(equalTo(1)));
    assertThat(node.getDataDirs().orDefault().get("main"), is(equalTo(RawPath.valueOf("foo/bar"))));

    node = Testing.newTestNode("node1", "localhost");
    DATA_DIRS.setProperty(node, null, "main:foo/bar");
    assertThat(node.getDataDirs().orDefault().size(), is(equalTo(1)));
    assertThat(node.getDataDirs().orDefault().get("main"), is(equalTo(RawPath.valueOf("foo/bar"))));

    // linux mapping
    node = Testing.newTestNode("node1", "localhost");
    DATA_DIRS.setProperty(node, null, "main:foo/bar,second:foo/baz");
    assertThat(node.getDataDirs().orDefault().size(), is(equalTo(2)));
    assertThat(node.getDataDirs().orDefault().get("main"), is(equalTo(RawPath.valueOf("foo/bar"))));
    assertThat(node.getDataDirs().orDefault().get("second"), is(equalTo(RawPath.valueOf("foo/baz"))));

    // win mapping
    node = Testing.newTestNode("node1", "localhost");
    DATA_DIRS.setProperty(node, null, "main:foo\\bar,second:foo\\baz");
    assertThat(node.getDataDirs().orDefault().size(), is(equalTo(2)));
    assertThat(node.getDataDirs().orDefault().get("main"), is(equalTo(RawPath.valueOf("foo\\bar"))));
    assertThat(node.getDataDirs().orDefault().get("second"), is(equalTo(RawPath.valueOf("foo\\baz"))));
  }

  @Test
  public void test_setProperty_TC_PROPERTIES() {
    Node node = Testing.newTestNode("node1", "localhost").putTcProperty("foo", "bar");
    assertThat(node.getTcProperties().orDefault().size(), is(equalTo(1)));

    TC_PROPERTIES.setProperty(node, null);
    assertThat(node.getTcProperties().orDefault().size(), is(equalTo(0)));

    node = Testing.newTestNode("node1", "localhost").putTcProperty("foo", "bar");
    TC_PROPERTIES.setProperty(node, "");
    assertThat(node.getTcProperties().orDefault().size(), is(equalTo(0)));

    node = Testing.newTestNode("node1", "localhost").putTcProperty("foo", "bar");
    TC_PROPERTIES.setProperty(node, null, null);
    assertThat(node.getTcProperties().orDefault().size(), is(equalTo(0)));

    node = Testing.newTestNode("node1", "localhost").putTcProperty("main", "bar");
    TC_PROPERTIES.setProperty(node, "main", null);
    assertThat(node.getTcProperties().orDefault().size(), is(equalTo(0)));

    node = Testing.newTestNode("node1", "localhost").putTcProperty("main", "bar");
    TC_PROPERTIES.setProperty(node, "main", "baz");
    assertThat(node.getTcProperties().orDefault().size(), is(equalTo(1)));
    assertThat(node.getTcProperties().orDefault().get("main"), is(equalTo("baz")));

    node = Testing.newTestNode("node1", "localhost").putTcProperty("main", "bar");
    TC_PROPERTIES.setProperty(node, null, "main:baz");
    assertThat(node.getTcProperties().orDefault().size(), is(equalTo(1)));
    assertThat(node.getTcProperties().orDefault().get("main"), is(equalTo("baz")));

    node = Testing.newTestNode("node1", "localhost").putTcProperty("main", "bar");
    TC_PROPERTIES.setProperty(node, null, "main:baz1,second:baz1");
    assertThat(node.getTcProperties().orDefault().size(), is(equalTo(2)));
    assertThat(node.getTcProperties().orDefault().get("main"), is(equalTo("baz1")));
    assertThat(node.getTcProperties().orDefault().get("second"), is(equalTo("baz1")));
  }

  @Test
  public void test_setProperty_NODE_LOGGER_OVERRIDES() {
    Node node = Testing.newTestNode("node1", "localhost").putLoggerOverride("com.foo", "TRACE");
    assertThat(node.getLoggerOverrides().orDefault().size(), is(equalTo(1)));

    NODE_LOGGER_OVERRIDES.setProperty(node, null);
    assertThat(node.getLoggerOverrides().orDefault().size(), is(equalTo(0)));

    node = Testing.newTestNode("node1", "localhost").putLoggerOverride("foo", "TRACE");
    NODE_LOGGER_OVERRIDES.setProperty(node, null, null);
    assertThat(node.getLoggerOverrides().orDefault().size(), is(equalTo(0)));

    node = Testing.newTestNode("node1", "localhost").putLoggerOverride("com.foo", "TRACE");
    NODE_LOGGER_OVERRIDES.setProperty(node, "com.foo", null);
    assertThat(node.getLoggerOverrides().orDefault().size(), is(equalTo(0)));

    node = Testing.newTestNode("node1", "localhost").putLoggerOverride("com.foo", "TRACE");
    NODE_LOGGER_OVERRIDES.setProperty(node, "com.foo", "INFO");
    assertThat(node.getLoggerOverrides().orDefault().size(), is(equalTo(1)));
    assertThat(node.getLoggerOverrides().orDefault().get("com.foo"), is("INFO"));

    node = Testing.newTestNode("node1", "localhost").putLoggerOverride("com.foo", "TRACE");
    NODE_LOGGER_OVERRIDES.setProperty(node, null, "com.foo:INFO");
    assertThat(node.getLoggerOverrides().orDefault().size(), is(equalTo(1)));
    assertThat(node.getLoggerOverrides().orDefault().get("com.foo"), is("INFO"));

    node = Testing.newTestNode("node1", "localhost").putLoggerOverride("com.foo", "TRACE");
    NODE_LOGGER_OVERRIDES.setProperty(node, null, "com.foo:INFO,com.bar:WARN");
    assertThat(node.getLoggerOverrides().orDefault().size(), is(equalTo(2)));
    assertThat(node.getLoggerOverrides().orDefault().get("com.foo"), is("INFO"));
    assertThat(node.getLoggerOverrides().orDefault().get("com.bar"), is("WARN"));
  }
}