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
package org.terracotta.management.entity.nms.agent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;

import java.io.Serializable;

/**
 * @author Mathieu Carbou
 */
@SuppressFBWarnings("SE_BAD_FIELD")
public class ReconnectData implements Serializable {

  private static final long serialVersionUID = 2L;

  public final Context root;
  public final String[] tags;
  public final ContextContainer contextContainer;
  public final Capability[] capabilities;
  public final ContextualNotification contextualNotification;

  public ReconnectData(String[] tags, Context root, ContextContainer contextContainer, Capability[] capabilities, ContextualNotification contextualNotification) {
    this.tags = tags;
    this.contextContainer = contextContainer;
    this.root = root;
    this.capabilities = capabilities;
    this.contextualNotification = contextualNotification;
  }
}
