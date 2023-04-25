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
package org.terracotta.management.entity.sample.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.terracotta.json.Json;
import org.terracotta.management.model.capabilities.context.CapabilityContext;

import java.util.Collection;

/**
 * @author Mathieu Carbou
 */
public class TestModule extends SimpleModule implements Json.Module {
  private static final long serialVersionUID = 1L;

  public TestModule() {
    super(TestModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

    setMixInAnnotation(CapabilityContext.class, CapabilityContextMixin.class);
  }

  public static abstract class CapabilityContextMixin {
    @JsonIgnore
    public abstract Collection<String> getRequiredAttributeNames();

    @JsonIgnore
    public abstract Collection<CapabilityContext.Attribute> getRequiredAttributes();
  }

}
