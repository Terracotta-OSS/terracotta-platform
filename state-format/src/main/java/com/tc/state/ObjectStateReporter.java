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
package com.tc.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.terracotta.entity.StateDumpCollector;

/**
 *
 */
public class ObjectStateReporter {
  private final StateDumpCollector collector;
  private final ObjectMapper mapper;
  private final ObjectNode object;

  public ObjectStateReporter(StateDumpCollector collector) {
    this.collector = collector;
    this.mapper = new ObjectMapper();
    this.object = mapper.createObjectNode();
  }
  
  public ObjectNode getBaseObject() {
    return object;
  }
  
  public void finish() {
    try {
      collector.addState(StateDumpCollector.JSON_STATE_KEY, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this.object));
    } catch (IOException ioe) {
      collector.addState("error", ioe.getMessage());
    }
  }
}
