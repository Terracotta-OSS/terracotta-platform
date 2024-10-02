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
package org.terracotta.json.gson.internal;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.annotations.Expose;

enum ExclusionStrategies implements ExclusionStrategy {
  EXPOSE_ANNOTATION_SERIALIZE {
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
      return f.getAnnotation(Expose.class) != null && !f.getAnnotation(Expose.class).serialize();
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
      return false;
    }
  },

  EXPOSE_ANNOTATION_DESERIALIZE {
    @Override
    public boolean shouldSkipField(FieldAttributes f) {
      return f.getAnnotation(Expose.class) != null && !f.getAnnotation(Expose.class).deserialize();
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
      return false;
    }
  }
}
