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
package com.tc.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.tc.classloader.BuiltinService;
import com.tc.text.PrettyPrinter;
import java.util.Collection;
import java.util.Collections;
import org.terracotta.entity.PlatformConfiguration;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;


/**
 *
 */
@BuiltinService
public class ReportingServiceProvider implements ServiceProvider {

  @Override
  public boolean initialize(ServiceProviderConfiguration spc, PlatformConfiguration pc) {
    return true;
  }

  @Override
  public <T> T getService(long l, ServiceConfiguration<T> sc) {
    if (com.tc.text.PrettyPrinter.class.isAssignableFrom(sc.getServiceType())) {
      ObjectMapper mapper = new ObjectMapper();
      ArrayNode array = mapper.createArrayNode();
      return (T)new com.tc.text.PrettyPrinter() {
        @Override
        public PrettyPrinter println(Object o) {
          array.addPOJO(o);
          return this;
        }

        @Override
        public void flush() {
          
        }

        @Override
        public String toString() {
          try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(array);
          } catch (JsonProcessingException jp) {
            jp.printStackTrace();
            return "";
          }
        }
      };
    }
    return null;
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return Collections.singleton(PrettyPrinter.class);
  }

  @Override
  public void prepareForSynchronization() throws ServiceProviderCleanupException {

  }
  
}
