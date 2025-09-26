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
package org.terracotta.diagnostic.server.extensions;

import java.time.Instant;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.terracotta.common.struct.Version;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_SERVER;
import org.terracotta.diagnostic.model.KitInformation;
import org.terracotta.server.ServerJMX;

public class DiagnosticExtensionsMBeanImplTest {

  private ServerJMX serverJMX;
  private DiagnosticExtensionsMBeanImpl diagnosticExtensionsMBean;

  @Before
  public void setUp() {
    serverJMX = mock(ServerJMX.class);
    diagnosticExtensionsMBean = new DiagnosticExtensionsMBeanImpl(serverJMX);
  }

  @Test
  public void testGetKitInformation_StandardDateFormat() {
    // Setup
    String version = "Terracotta 5.8.2";
    String buildId = "2021-06-29 at 20:54:46 UTC (Revision abc123 from main-branch)";

    when(serverJMX.call(MBEAN_SERVER, "getVersion", null)).thenReturn(version);
    when(serverJMX.call(MBEAN_SERVER, "getBuildID", null)).thenReturn(buildId);

    // Execute
    KitInformation kitInfo = diagnosticExtensionsMBean.getKitInformation();

    // Verify
    assertThat(kitInfo.getVersion(), is(equalTo(Version.valueOf("5.8.2"))));
    assertThat(kitInfo.getRevision(), is(equalTo("abc123")));
    assertThat(kitInfo.getBranch(), is(equalTo("main-branch")));

    // Verify timestamp - 2021-06-29 at 20:54:46 UTC
    Instant expectedTimestamp = Instant.parse("2021-06-29T20:54:46Z");
    assertThat(kitInfo.getTimestamp(), is(equalTo(expectedTimestamp)));
  }

  @Test
  public void testGetKitInformation_ISODateFormat() {
    // Setup
    String version = "Terracotta 5.8.3";
    String buildId = "2021-07-15T14:30:00Z (Revision def456 from feature-branch)";

    when(serverJMX.call(MBEAN_SERVER, "getVersion", null)).thenReturn(version);
    when(serverJMX.call(MBEAN_SERVER, "getBuildID", null)).thenReturn(buildId);

    // Execute
    KitInformation kitInfo = diagnosticExtensionsMBean.getKitInformation();

    // Verify
    assertThat(kitInfo.getVersion(), is(equalTo(Version.valueOf("5.8.3"))));
    assertThat(kitInfo.getRevision(), is(equalTo("def456")));
    assertThat(kitInfo.getBranch(), is(equalTo("feature-branch")));

    // Verify timestamp - ISO format
    Instant expectedTimestamp = Instant.parse("2021-07-15T14:30:00Z");
    assertThat(kitInfo.getTimestamp(), is(equalTo(expectedTimestamp)));
  }

  @Test
  public void testGetKitInformation_InvalidDateFormat() {
    // Setup
    String version = "Terracotta 5.8.4";
    String buildId = "Invalid-Date-Format (Revision ghi789 from bugfix-branch)";

    when(serverJMX.call(MBEAN_SERVER, "getVersion", null)).thenReturn(version);
    when(serverJMX.call(MBEAN_SERVER, "getBuildID", null)).thenReturn(buildId);

    // Execute
    KitInformation kitInfo = diagnosticExtensionsMBean.getKitInformation();

    // Verify
    assertThat(kitInfo.getVersion(), is(equalTo(Version.valueOf("5.8.4"))));
    assertThat(kitInfo.getRevision(), is(equalTo("ghi789")));
    assertThat(kitInfo.getBranch(), is(equalTo("bugfix-branch")));

    // Verify timestamp - should default to epoch 0 for invalid format
    Instant expectedTimestamp = Instant.ofEpochMilli(0L);
    assertThat(kitInfo.getTimestamp(), is(equalTo(expectedTimestamp)));
  }

  @Test
  public void testGetKitInformation_NoSpaceInVersion() {
    // Setup
    String version = "5.8.5-SNAPSHOT";
    String buildId = "2021-08-01 at 10:00:00 UTC (Revision jkl012 from dev-branch)";

    when(serverJMX.call(MBEAN_SERVER, "getVersion", null)).thenReturn(version);
    when(serverJMX.call(MBEAN_SERVER, "getBuildID", null)).thenReturn(buildId);

    // Execute
    KitInformation kitInfo = diagnosticExtensionsMBean.getKitInformation();

    // Verify
    assertThat(kitInfo.getVersion(), is(equalTo(Version.valueOf("5.8.5-SNAPSHOT"))));
    assertThat(kitInfo.getRevision(), is(equalTo("jkl012")));
    assertThat(kitInfo.getBranch(), is(equalTo("dev-branch")));

    // Verify timestamp
    Instant expectedTimestamp = Instant.parse("2021-08-01T10:00:00Z");
    assertThat(kitInfo.getTimestamp(), is(equalTo(expectedTimestamp)));
  }

  @Test
  public void testGetKitInformation_UnknownBuildInfo() {
    // Setup
    String version = "Terracotta 5.9.0";
    String buildId = "Something completely different";

    when(serverJMX.call(MBEAN_SERVER, "getVersion", null)).thenReturn(version);
    when(serverJMX.call(MBEAN_SERVER, "getBuildID", null)).thenReturn(buildId);

    // Execute
    KitInformation kitInfo = diagnosticExtensionsMBean.getKitInformation();

    // Verify
    assertThat(kitInfo.getVersion(), is(equalTo(Version.valueOf("5.9.0"))));
    assertThat(kitInfo.getRevision(), is(equalTo("UNKNOWN")));
    assertThat(kitInfo.getBranch(), is(equalTo("UNKNOWN")));

    // Verify timestamp - should default to epoch 0 for unmatched pattern
    Instant expectedTimestamp = Instant.ofEpochMilli(0L);
    assertThat(kitInfo.getTimestamp(), is(equalTo(expectedTimestamp)));
  }
}

// Made with Bob
