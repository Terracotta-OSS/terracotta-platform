/*
 * Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.terracotta.management.model.cluster;

import org.terracotta.management.model.context.Context;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
public interface ManageableContainer<B> {
  Map<String, Manageable> getManageables();

  int getManageableCount();

  B addManageable(Manageable manageable);

  Stream<Manageable> manageableStream();

  Optional<Manageable> getManageable(Context context);

  Optional<Manageable> getManageable(String id);

  Optional<Manageable> getManageable(String name, String type);

  boolean hasManageable(String name, String type);

  Optional<Manageable> removeManageable(String id);

  Stream<Manageable> manageableStream(String type);
}
