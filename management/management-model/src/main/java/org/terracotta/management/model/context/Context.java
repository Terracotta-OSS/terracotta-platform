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
package org.terracotta.management.model.context;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Ludovic Orban
 * @author Mathieu Carbou
 */
public class Context extends AbstractMap<String, String> implements Serializable {

  private static final long serialVersionUID = 1;

  private final Map<String, String> back = new LinkedHashMap<>();

  private Context(Map<String, String> back) {
    this.back.putAll(back);
  }

  public Map<String, String> toMap() {
    return Collections.unmodifiableMap(back);
  }

  public Context without(String key) {
    Context context = new Context(back);
    context.back.remove(key);
    return context;
  }

  public Context with(String key, String val) {
    if (val == null) {
      throw new NullPointerException();
    }
    Context context = new Context(back);
    context.back.put(key, val);
    return context;
  }

  public Context with(Map<String, String> props) {
    for (String val : props.values()) {
      if (val == null) {
        throw new NullPointerException();
      }
    }
    Context context = new Context(back);
    context.back.putAll(props);
    return context;
  }

  public String get(String key) {
    return back.get(key);
  }

  public int size() {
    return back.size();
  }

  public boolean isEmpty() {return back.isEmpty();}

  @Override
  public Set<Entry<String, String>> entrySet() {
    return toMap().entrySet();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Context context = (Context) o;
    return back.equals(context.back);
  }

  @Override
  public int hashCode() {
    return back.hashCode();
  }

  @Override
  public String toString() {
    return back.toString();
  }

  public boolean contains(Context subCtx) {
    return back.entrySet().containsAll(subCtx.back.entrySet());
  }

  public boolean contains(String key) {
    return back.containsKey(key);
  }

  public boolean contains(String key, String val) {
    return back.containsKey(key) && back.get(key).equals(val);
  }

  public static Context create(String key, String val) {
    return empty().with(key, val);
  }

  public static Context create(Map<String, String> map) {
    return new Context(map);
  }

  public static Context empty() {
    return new Context(Collections.<String, String>emptyMap());
  }
}
