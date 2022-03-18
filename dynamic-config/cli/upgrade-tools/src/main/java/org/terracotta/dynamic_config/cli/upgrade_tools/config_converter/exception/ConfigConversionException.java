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
package org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.exception;

import org.terracotta.common.struct.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ConfigConversionException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final Map<String, List<String>> parameters = new HashMap<>();
  private final ErrorCode errorCode;

  @SafeVarargs
  public ConfigConversionException(ErrorCode errorCode, final String s, Tuple2<String, String>... params) {
    super(s);
    this.errorCode = errorCode;
    if (params != null) {
      @SuppressWarnings("varargs")
      Stream<Tuple2<String, String>> paramStream = Arrays.stream(params);
      paramStream.forEach(param -> {
        parameters.compute(param.getT1(), (key, value) -> {
          List<String> retValue;
          if (value == null) {
            retValue = new ArrayList<>();
          } else {
            retValue = value;
          }
          retValue.add(param.getT2());
          return retValue;
        });
      });
    }
  }

  public Map<String, List<String>> getParameters() {
    return Collections.unmodifiableMap(parameters);
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}