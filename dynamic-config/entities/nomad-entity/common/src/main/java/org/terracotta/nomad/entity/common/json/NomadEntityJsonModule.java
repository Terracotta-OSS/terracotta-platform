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
package org.terracotta.nomad.entity.common.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.json.Json;
import org.terracotta.nomad.entity.common.NomadEntityMessage;
import org.terracotta.nomad.entity.common.NomadEntityResponse;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.MutativeMessage;

import static java.util.Collections.singletonList;

/**
 * @author Mathieu Carbou
 */
public class NomadEntityJsonModule extends SimpleModule implements Json.Module {
  private static final long serialVersionUID = 1L;

  public NomadEntityJsonModule() {
    super(NomadEntityJsonModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

    setMixInAnnotation(NomadEntityMessage.class, NomadEntityMessageMixin.class);
    setMixInAnnotation(NomadEntityResponse.class, NomadEntityResponseMixin.class);
  }

  @Override
  public Iterable<? extends Module> getDependencies() {
    return singletonList(new DynamicConfigApiJsonModule());
  }

  public static class NomadEntityResponseMixin extends NomadEntityResponse {
    @JsonCreator
    protected NomadEntityResponseMixin(@JsonProperty(value = "response", required = true) AcceptRejectResponse response) {
      super(response);
    }
  }

  public static class NomadEntityMessageMixin extends NomadEntityMessage {
    @JsonCreator
    protected NomadEntityMessageMixin(@JsonProperty(value = "nomadMessage", required = true) MutativeMessage message) {
      super(message);
    }
  }
}
