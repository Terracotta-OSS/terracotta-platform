/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.diagnostic.model;

import org.terracotta.common.struct.Version;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class KitInformation {

  private final Version version;
  private final String revision;
  private final String branch;
  private final Instant timestamp;

  public KitInformation(Version version, String revision, String branch, Instant timestamp) {
    this.version = requireNonNull(version);
    this.revision = requireNonNull(revision);
    this.branch = requireNonNull(branch);
    this.timestamp = requireNonNull(timestamp);
  }

  public Version getVersion() {
    return version;
  }

  public String getRevision() {
    return revision;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getBranch() {
    return branch;
  }

  public String getFormattedVersion() {
    return version + "/" + (revision.isEmpty() ? "revision not available" : revision);
  }

  public Properties toProperties() {
    Properties props = new Properties();
    props.setProperty("version", getVersion().toString());
    props.setProperty("revision", getRevision());
    props.setProperty("branch", getBranch());
    props.setProperty("timestamp", getTimestamp().toString());
    return props;
  }

  @Override
  public String toString() {
    try {
      StringWriter sw = new StringWriter();
      toProperties().store(sw, null);
      return sw.toString();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static KitInformation fromProperties(String props) {
    try {
      Properties p = new Properties();
      p.load(new StringReader(props));
      return fromProperties(p);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static KitInformation fromProperties(Properties props) {
    return new KitInformation(
        Version.valueOf(props.getProperty("version")),
        props.getProperty("revision"),
        props.getProperty("branch"),
        Instant.parse(props.getProperty("timestamp")));
  }
}
