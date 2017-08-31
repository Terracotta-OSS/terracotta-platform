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
package org.terracotta.client.message.tracker;

import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.ServiceConfiguration;

import com.tc.classloader.CommonComponent;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

@CommonComponent
public class OOOMessageHandlerConfiguration<M extends EntityMessage, R extends EntityResponse> implements ServiceConfiguration<OOOMessageHandler<M, R>> {

  private final String entityIdentifier;
  private final Predicate<M> trackerPolicy;
  private final int segments;
  private final ToIntFunction<M> segmentationStrategy;

  public OOOMessageHandlerConfiguration(String entityIdentifier, Predicate<M> trackerPolicy, int segments, ToIntFunction<M> segmentationStrategy) {
    if (segments <= 0) {
      throw new IllegalArgumentException("The segment size is a non-positive value: " + segments);
    }
    this.entityIdentifier = entityIdentifier;
    this.trackerPolicy = trackerPolicy;
    this.segments = segments;
    this.segmentationStrategy = segmentationStrategy;
  }

  public Predicate<M> getTrackerPolicy() {
    return trackerPolicy;
  }

  public String getEntityIdentifier() {
    return entityIdentifier;
  }

  public ToIntFunction<M> getSegmentationStrategy() {
    return segmentationStrategy;
  }

  public int getSegments() {
    return segments;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class<OOOMessageHandler<M, R>> getServiceType() {
    return (Class) OOOMessageHandler.class;
  }
}
