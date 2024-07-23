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
package org.terracotta.persistence.sanskrit;

import org.terracotta.json.DefaultJsonFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.terracotta.persistence.sanskrit.MarkableLineParser.LS;

public class LogUtil {
  @SafeVarargs
  public static LogInfo createLog(Map<String, Object>... records) throws Exception {
    StringBuilder log = new StringBuilder();
    String lastHash = null;

    List<String> texts = new ArrayList<>();
    List<String> hashes = new ArrayList<>();

    for (Map<String, Object> record : records) {
      String timestamp = Instant.now().toString();
      String data = new DefaultJsonFactory().create().toString(record);

      String entryString = timestamp + LS + data;

      String hashString = "";
      if (lastHash != null) {
        hashString = lastHash + LS + LS;
      }

      hashString += entryString;

      lastHash = HashUtils.generateHash(hashString.getBytes(StandardCharsets.UTF_8));

      log.append(entryString);
      log.append(LS);
      log.append(lastHash);
      log.append(LS);
      log.append(LS);

      texts.add(log.toString());
      hashes.add(HashUtils.generateHash(lastHash));
    }

    return new LogInfo(texts, hashes);
  }
}
