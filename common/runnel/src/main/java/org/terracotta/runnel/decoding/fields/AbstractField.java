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
package org.terracotta.runnel.decoding.fields;

/**
 * @author Ludovic Orban
 */
public abstract class AbstractField implements Field {

  private final String name;
  private final int index;

  protected AbstractField(String name, int index) {
    this.name = name;
    this.index = index;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public int index() {
    return index;
  }

}
