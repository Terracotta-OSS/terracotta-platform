/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.persistence.sanskrit.change;

import org.terracotta.persistence.sanskrit.SanskritObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows construction of a change to be applied atomically to the append log.
 */
public class SanskritChangeBuilder {
  private final List<SanskritChange> changes = new ArrayList<>();

  public static SanskritChangeBuilder newChange() {
    return new SanskritChangeBuilder();
  }

  private SanskritChangeBuilder() {}

  public SanskritChangeBuilder setString(String key, String value) {
    changes.add(new AddStringSanskritChange(key, value));
    return this;
  }

  public SanskritChangeBuilder setLong(String key, long value) {
    changes.add(new AddLongSanskritChange(key, value));
    return this;
  }

  public SanskritChangeBuilder setObject(String key, SanskritObject value) {
    changes.add(new AddObjectSanskritChange(key, value));
    return this;
  }

  public SanskritChangeBuilder removeKey(String key) {
    changes.add(new UnsetKeySanskritChange(key));
    return this;
  }

  public SanskritChange build() {
    return new MuxSanskritChange(changes);
  }
}
