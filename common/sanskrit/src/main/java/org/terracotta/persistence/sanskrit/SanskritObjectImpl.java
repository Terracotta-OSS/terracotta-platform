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

import java.util.LinkedHashMap;
import java.util.Map;

public class SanskritObjectImpl implements MutableSanskritObject {
  private final LinkedHashMap<String, Object> mappings;
  private final SanskritMapper mapper;

  public SanskritObjectImpl(SanskritMapper mapper) {
    this.mapper = mapper;
    this.mappings = new LinkedHashMap<>();
  }

  @Override
  public void setString(String key, String value) {
    mappings.put(key, value);
  }

  @Override
  public void setLong(String key, long value) {
    mappings.put(key, value);
  }

  @Override
  public void setObject(String key, SanskritObject value) throws SanskritException {
    if (value == null) {
      mappings.put(key, null);
    } else {
      SanskritObjectImpl copy = new SanskritObjectImpl(mapper);
      value.accept(copy);
      mappings.put(key, copy);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void set(String key, Object value, String version) throws SanskritException {
    if (value instanceof String) {
      setString(key, (String) value);
    } else if (value instanceof Long) {
      setLong(key, (Long) value);
    } else if (value instanceof SanskritObject) {
      setObject(key, (SanskritObject) value);
    } else if (value instanceof Map) {
      SanskritObjectImpl o = new SanskritObjectImpl(mapper);
      for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
        o.set(entry.getKey(), entry.getValue(), version);
      }
      mappings.put(key, o);
    } else {
      // anything else (incl. value == null || value instanceof Number)
      mappings.put(key, value);
    }
  }

  @Override
  public void accept(SanskritVisitor visitor) throws SanskritException {
    for (Map.Entry<String, Object> entry : mappings.entrySet()) {
      visitor.set(entry.getKey(), entry.getValue(), mapper.getCurrentFormatVersion());
    }
  }

  @Override
  public <T> T get(String key, Class<T> type, String version) throws SanskritException {
    if (type == Long.class) {
      return type.cast(getLong(key));
    } else if (type == String.class) {
      return type.cast(getString(key));
    } else if (type == SanskritObject.class) {
      return type.cast(getObject(key));
    } else {
      // type == complex type ?
      final Object o = mappings.get(key);
      if (o == null) {
        return null;
      } else if (type.isInstance(o)) {
        return type.cast(o);
      } else if (o instanceof SanskritObject) {
        return mapper.map((SanskritObject) o, type, version);
      } else {
        throw new AssertionError("Unsupported: Cannot convert to: " + type + " key: " + key + " with value: " + o + " of type: " + o.getClass());
      }
    }
  }

  @Override
  public String getString(String key) {
    return (String) mappings.get(key);
  }

  @Override
  public Long getLong(String key) {
    final Number o = (Number) mappings.get(key);
    return o == null ? null : o.longValue();
  }

  @Override
  public SanskritObject getObject(String key) throws SanskritException {
    final SanskritObject o = (SanskritObject) mappings.get(key);
    if (o == null) {
      return null;
    }
    SanskritObjectImpl copy = new SanskritObjectImpl(mapper);
    o.accept(copy);
    return copy;
  }

  @Override
  public void removeKey(String key) {
    mappings.remove(key);
  }

  @Override
  public String toString() {
    return mappings.toString();
  }
}
