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

import java.util.List;

public class LogInfo {
  private final List<String> texts;
  private final List<String> hashes;

  public LogInfo(List<String> texts, List<String> hashes) {
    this.texts = texts;
    this.hashes = hashes;
  }

  public String getText() {
    return texts.get(texts.size() - 1);
  }

  public String getHash() {
    return hashes.get(hashes.size() - 1);
  }

  public String getText(int record) {
    return texts.get(record);
  }

  public String getHash(int record) {
    return hashes.get(record);
  }
}
