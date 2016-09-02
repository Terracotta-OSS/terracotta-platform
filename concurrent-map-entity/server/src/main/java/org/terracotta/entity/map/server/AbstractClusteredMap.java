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
package org.terracotta.entity.map.server;

import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.CommonServerEntity;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.map.common.BooleanResponse;
import org.terracotta.entity.map.common.ConditionalRemoveOperation;
import org.terracotta.entity.map.common.ConditionalReplaceOperation;
import org.terracotta.entity.map.common.ContainsKeyOperation;
import org.terracotta.entity.map.common.ContainsValueOperation;
import org.terracotta.entity.map.common.EntrySetResponse;
import org.terracotta.entity.map.common.GetOperation;
import org.terracotta.entity.map.common.KeySetResponse;
import org.terracotta.entity.map.common.MapOperation;
import org.terracotta.entity.map.common.MapResponse;
import org.terracotta.entity.map.common.MapValueResponse;
import org.terracotta.entity.map.common.NullResponse;
import org.terracotta.entity.map.common.PutAllOperation;
import org.terracotta.entity.map.common.PutIfAbsentOperation;
import org.terracotta.entity.map.common.PutIfPresentOperation;
import org.terracotta.entity.map.common.PutOperation;
import org.terracotta.entity.map.common.RemoveOperation;
import org.terracotta.entity.map.common.SizeResponse;
import org.terracotta.entity.map.common.ValueCollectionResponse;
import org.terracotta.service.reference.holder.ReferenceHolderService;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AbstractClusteredMap implements CommonServerEntity<MapOperation, MapResponse> {

  private static final String DATA_MAP_REF = "dataMap";

  protected final ReferenceHolderService referenceHolderService;

  protected volatile ConcurrentMap<Object, Object> dataMap;

  public AbstractClusteredMap(ServiceRegistry services) {
    this.referenceHolderService = services.getService(new BasicServiceConfiguration<ReferenceHolderService>(ReferenceHolderService.class));
  }

  @Override
  public void createNew() {
    this.dataMap = referenceHolderService.storeReference(DATA_MAP_REF, new ConcurrentHashMap());
  }

  @Override
  public void loadExisting() {
    this.dataMap = referenceHolderService.retrieveReference(DATA_MAP_REF, ConcurrentMap.class);
  }

  @Override
  public void destroy() {
    dataMap.clear();
  }

  MapResponse invokeInternal(MapOperation input) {
    MapResponse response;

    switch (input.operationType()) {
      case PUT: {
        PutOperation putOperation = (PutOperation) input;
        Object key = putOperation.getKey();
        Object old = dataMap.get(key);
        dataMap.put(key, putOperation.getValue());
        response = new MapValueResponse(old);
        break;
      }
      case GET: {
        Object key = ((GetOperation) input).getKey();
        response = new MapValueResponse(dataMap.get(key));
        break;
      }
      case REMOVE: {
        Object key = ((RemoveOperation) input).getKey();
        response = new MapValueResponse(dataMap.remove(key));
        break;
      }
      case CONTAINS_KEY: {
        Object key = ((ContainsKeyOperation) input).getKey();
        response = new BooleanResponse(dataMap.containsKey(key));
        break;
      }
      case CONTAINS_VALUE: {
        Object value = ((ContainsValueOperation) input).getValue();
        response = new BooleanResponse(dataMap.containsValue(value));
        break;
      }
      case CLEAR: {
        dataMap.clear();
        // There is no response from the clear.
        response = new NullResponse();
        break;
      }
      case PUT_ALL: {
        @SuppressWarnings("unchecked")
        Map<Object, Object> newValues = (Map<Object, Object>) ((PutAllOperation)input).getMap();
        dataMap.putAll(newValues);
        // There is no response from a put all.
        response = new NullResponse();
        break;
      }
      case KEY_SET: {
        Set<Object> keySet = new HashSet<Object>();
        keySet.addAll(dataMap.keySet());
        response = new KeySetResponse(keySet);
        break;
      }
      case VALUES: {
        Collection<Object> values = new ArrayList<Object>();
        values.addAll(dataMap.values());
        response = new ValueCollectionResponse(values);
        break;
      }
      case ENTRY_SET: {
        Set<Map.Entry<Object, Object>> entrySet = new HashSet<Map.Entry<Object, Object>>();
        for (Map.Entry<Object, Object> entry : dataMap.entrySet()) {
          entrySet.add(new AbstractMap.SimpleEntry<Object, Object > (entry.getKey(), entry.getValue()));
        }
        response = new EntrySetResponse(entrySet);
        break;
      }
      case SIZE: {
        response = new SizeResponse(dataMap.size());
        break;
      }
      case PUT_IF_ABSENT: {
        PutIfAbsentOperation operation = (PutIfAbsentOperation) input;
        response = new MapValueResponse(dataMap.putIfAbsent(operation.getKey(), operation.getValue()));
        break;
      }
      case PUT_IF_PRESENT: {
        PutIfPresentOperation operation = (PutIfPresentOperation) input;
        response = new MapValueResponse(dataMap.replace(operation.getKey(), operation.getValue()));
        break;
      }
      case CONDITIONAL_REMOVE: {
        ConditionalRemoveOperation operation = (ConditionalRemoveOperation) input;
        response = new BooleanResponse(dataMap.remove(operation.getKey(), operation.getValue()));
        break;
      }
      case CONDITIONAL_REPLACE: {
        ConditionalReplaceOperation operation = (ConditionalReplaceOperation) input;
        response = new BooleanResponse(dataMap.replace(operation.getKey(), operation.getOldValue(), operation.getNewValue()));
        break;
      }
      default:
        // Unknown message type.
        throw new AssertionError("Unsupported message type: " + input.operationType());
    }
    return response;
  }

}
