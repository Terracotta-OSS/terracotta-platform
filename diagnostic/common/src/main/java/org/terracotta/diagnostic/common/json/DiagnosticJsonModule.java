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
package org.terracotta.diagnostic.common.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.terracotta.diagnostic.common.DiagnosticRequest;
import org.terracotta.diagnostic.common.DiagnosticResponse;
import org.terracotta.json.Json;

import java.util.Optional;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticJsonModule extends SimpleModule implements Json.Module {
  private static final long serialVersionUID = 1L;

  public DiagnosticJsonModule() {
    super(DiagnosticJsonModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

    setMixInAnnotation(DiagnosticRequest.class, DiagnosticRequestMixin.class);
    setMixInAnnotation(DiagnosticResponse.class, DiagnosticResponseMixin.class);
  }

  @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
  public static class DiagnosticRequestMixin extends DiagnosticRequest {
    private static final long serialVersionUID = 1L;

    // Note: using "@class" here is fine since we control both end of input and output json plus the communication channel
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private final Object[] arguments;

    @JsonCreator
    public DiagnosticRequestMixin(@JsonProperty(value = "serviceInterface", required = true) Class<?> serviceInterface,
                                  @JsonProperty(value = "methodName", required = true) String methodName,
                                  @JsonProperty(value = "arguments") Object... arguments) {
      super(serviceInterface, methodName, arguments);
      this.arguments = arguments;
    }
  }

  @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
  public static class DiagnosticResponseMixin<T> extends DiagnosticResponse<T> {
    private static final long serialVersionUID = 1L;

    // Note: using "@class" here is fine since we control both end of input and output json plus the communication channel
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private final T body;

    @JsonCreator
    public DiagnosticResponseMixin(@JsonProperty("body") T body,
                                   @JsonProperty("errorType") String errorType,
                                   @JsonProperty("errorMessage") String errorMessage,
                                   @JsonProperty("errorStack") String errorStack) {
      super(body, errorType, errorMessage, errorStack);
      this.body = body;
    }

    @JsonIgnore
    public Optional<String> getError() {
      return super.getError();
    }

    @JsonIgnore
    public boolean hasError() {
      return super.hasError();
    }
  }
}
