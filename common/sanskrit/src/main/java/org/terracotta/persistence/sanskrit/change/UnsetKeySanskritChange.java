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
package org.terracotta.persistence.sanskrit.change;

/**
 * A data change corresponding to removing a mapping.
 */
public class UnsetKeySanskritChange implements SanskritChange {
  private final String key;

  public UnsetKeySanskritChange(String key) {
    this.key = key;
  }

  @Override
  public void accept(SanskritChangeVisitor visitor) {
    visitor.removeKey(key);
  }
}
