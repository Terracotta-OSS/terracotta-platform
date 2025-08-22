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

import java.util.Objects;

import static java.lang.String.format;

public class LockContext {
  private static final String DELIMITER = ";";

  private final String token;
  private final String ownerName;
  private final String ownerTags;

  // For Json
  private LockContext() {
    this(null, null, null);
  }

  public LockContext(String token, String ownerName, String ownerTags) {
    this.token = token;
    this.ownerName = ownerName;
    this.ownerTags = ownerTags;
  }

  public String getToken() {
    return token;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public String getOwnerTags() {
    return ownerTags;
  }

  public static LockContext from(String contextStr) {
    String[] substrings = contextStr.split(DELIMITER);

    if (substrings.length != 3) {
      throw new IllegalArgumentException(format("Invalid lock-context '%s', expected format 'uuid;owner-name;owner-tags", contextStr));
    }

    return new LockContext(substrings[0], substrings[1], substrings[2]);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final LockContext that = (LockContext) o;
    return Objects.equals(token, that.token) &&
        Objects.equals(ownerName, that.ownerName) &&
        Objects.equals(ownerTags, that.ownerTags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(token, ownerName, ownerTags);
  }

  public String toString() {
    return format("%s%s%s%s%s", token, DELIMITER, ownerName, DELIMITER, ownerTags);
  }

  public String ownerInfo() {
    return format("%s (%s)", ownerName, ownerTags);
  }
}
