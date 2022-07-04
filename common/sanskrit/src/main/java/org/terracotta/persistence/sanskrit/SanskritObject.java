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
package org.terracotta.persistence.sanskrit;

/**
 * An interface representing complex data to be held against a key.
 */
public interface SanskritObject {
  void accept(SanskritVisitor visitor);

  <T> T getObject(String key, Class<T> type, String version);

  String getString(String key);

  Long getLong(String key);

  SanskritObject getObject(String key);
}
