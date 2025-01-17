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
package org.terracotta.json.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import org.terracotta.json.gson.internal.RuntimeTypeAdapter;

/**
 * Type adapter that can be used to map a field without knowing its type in advance:
 * it will be serialized by using its runtime type and deserialize using its declared type.
 * <p>
 * This class can only be used on a mixin with {@link com.google.gson.annotations.JsonAdapter}
 */
public class RuntimeTypeAdapterFactory implements TypeAdapterFactory {
  // This construction is called when this class is used as an annotation placed on a field with type Object.
  private RuntimeTypeAdapterFactory() {
  }

  @Override
  public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> type) {
    // when placed on an annotated field, let's grab the adapter, if this is a runtime adapter, grab its delegate
    TypeAdapter<R> delegate = gson.getAdapter(type);
    if (delegate instanceof RuntimeTypeAdapter) {
      delegate = gson.getDelegateAdapter(this, type);
    }
    return new RuntimeTypeAdapter<>(gson, delegate, type);
  }

  @Override
  public String toString() {
    return "Factory[type=RUNTIME,adapter=" + RuntimeTypeAdapter.class.getName() + "]";
  }
}
