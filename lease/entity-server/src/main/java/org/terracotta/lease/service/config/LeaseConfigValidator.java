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
package org.terracotta.lease.service.config;

import org.terracotta.config.service.ConfigValidator;
import org.terracotta.config.service.ValidationException;
import org.w3c.dom.Element;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class LeaseConfigValidator implements ConfigValidator {

  private final Function<Element, LeaseElement> resourcesTypeSupplier;

  public LeaseConfigValidator(Function<Element, LeaseElement> resourcesTypeSupplier) {
    this.resourcesTypeSupplier = resourcesTypeSupplier;
  }

  public void validateAgainst(Element oneFragment, Element otherFragment) throws ValidationException {
    LeaseElement one = createObject(oneFragment);
    LeaseElement another = createObject(otherFragment);
    if (!compare(one, another)) {
      throw new ValidationException("Lease entries are not matching", ValidationFailureId.LEASE_ENTRY_MISMATCH.getFailureId());
    }
  }

  protected boolean compare(LeaseElement one, LeaseElement another) {

    TimeUnit oneTimeUnit = convert(one.getTimeUnit());
    TimeUnit anotherTimeUnit = convert(another.getTimeUnit());

    return TimeUnit.MILLISECONDS.convert(Long.parseLong(one.getLeaseValue()), oneTimeUnit)
           == TimeUnit.MILLISECONDS.convert(Long.parseLong(another.getLeaseValue()), anotherTimeUnit);
  }

  protected TimeUnit convert(String timeUnitString) {
    TimeUnit timeUnit = TimeUnit.valueOf(timeUnitString.toUpperCase());
    return timeUnit;
  }

  protected LeaseElement createObject(Element element) {
    return resourcesTypeSupplier.apply(element);
  }
}
