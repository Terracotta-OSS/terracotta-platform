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
package org.terracotta.json.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

import static java.util.Objects.requireNonNull;

public abstract class DelegateTypeAdapterFactory<T> implements TypeAdapterFactory {
  protected final TypeToken<T> type;
  protected final boolean matchSubTypes;
  protected final boolean isRawType;

  protected DelegateTypeAdapterFactory(TypeToken<T> type) {
    this(type, false);
  }

  protected DelegateTypeAdapterFactory(TypeToken<T> type, boolean matchSubTypes) {
    this.type = requireNonNull(type);
    this.matchSubTypes = matchSubTypes;
    this.isRawType = type.getType() instanceof Class<?>;
  }

  protected DelegateTypeAdapterFactory(Class<T> type) {
    this(TypeToken.get(type));
  }

  protected DelegateTypeAdapterFactory(Class<T> type, boolean matchSubTypes) {
    this(TypeToken.get(type), matchSubTypes);
  }

  @SuppressWarnings("unchecked")
  @Override
  public final <R> TypeAdapter<R> create(Gson gson, TypeToken<R> type) {
    if (matches(type)) {
      return (TypeAdapter<R>) Adapters.nullSafe(create(gson));
    }
    return null;
  }

  @Override
  public String toString() {
    return "Factory[type=" + type + "]";
  }

  protected <R> boolean matches(TypeToken<R> type) {
    final Class<? super T> thisRawType = this.type.getRawType();
    final Class<? super R> rawType = type.getRawType();
    return this.type.equals(type)
        || isRawType && type.getType() instanceof Class<?> && thisRawType == rawType
        || matchSubTypes && thisRawType.isAssignableFrom(rawType);
  }

  protected abstract TypeAdapter<T> create(Gson gson);
}
