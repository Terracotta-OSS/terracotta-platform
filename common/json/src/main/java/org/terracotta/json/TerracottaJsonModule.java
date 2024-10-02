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
package org.terracotta.json;

import com.google.gson.internal.bind.TypeAdapters;
import org.terracotta.json.gson.Adapters;
import org.terracotta.json.gson.GsonConfig;
import org.terracotta.json.gson.GsonModule;

/**
 * @author Mathieu Carbou
 */
public class TerracottaJsonModule implements GsonModule {
  @Override
  public void configure(GsonConfig config) {
    config.getBuilder().setObjectToNumberStrategy(Adapters.JACKSON_LIKE_NUMBER_STRATEGY);
    config.getBuilder().setNumberToNumberStrategy(Adapters.JACKSON_LIKE_NUMBER_STRATEGY);

    // numbers
    config.registerTypeAdapterFactory(TypeAdapters.newFactory(double.class, Double.class, Adapters.DOUBLES));
    config.registerTypeAdapterFactory(TypeAdapters.newFactory(float.class, Float.class, Adapters.FLOATS));
  }
}
