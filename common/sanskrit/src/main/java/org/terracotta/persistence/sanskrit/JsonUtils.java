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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import java.io.IOException;
import java.util.Map;

public class JsonUtils {
  public static void parse(ObjectMapperSupplier objectMapperSupplier, String version, String json, MutableSanskritObject result) throws SanskritException {
    try {
      JsonNode jsonNode = objectMapperSupplier.getObjectMapper(version).readTree(json);
      jsonNodeToSanskritObject(objectMapperSupplier, version, result, jsonNode);
    } catch (IOException e) {
      throw new SanskritException(e);
    }
  }

  private static void jsonNodeToSanskritObject(ObjectMapperSupplier objectMapperSupplier, String version, MutableSanskritObject sanskritObject, JsonNode jsonNode) throws SanskritException {
    for (Map.Entry<String, JsonNode> field : (Iterable<Map.Entry<String, JsonNode>>) jsonNode::fields) {
      String key = field.getKey();
      JsonNode value = field.getValue();

      JsonNodeType nodeType = value.getNodeType();
      switch (nodeType) {
        case NUMBER:
          sanskritObject.setLong(key, value.longValue());
          break;
        case STRING:
          sanskritObject.setString(key, value.textValue());
          break;
        case OBJECT:
          sanskritObject.setObject(key, jsonNodeToSanskritObject(objectMapperSupplier, version, value));
          break;
        case NULL:
          sanskritObject.removeKey(key);
          break;
        default:
          sanskritObject.setExternal(key, value, version);
      }
    }
  }

  private static MutableSanskritObject jsonNodeToSanskritObject(ObjectMapperSupplier objectMapperSupplier, String version, JsonNode jsonNode) throws SanskritException {
    SanskritObjectImpl sanskritObject = new SanskritObjectImpl(objectMapperSupplier);
    jsonNodeToSanskritObject(objectMapperSupplier, version, sanskritObject, jsonNode);
    return sanskritObject;
  }
}
