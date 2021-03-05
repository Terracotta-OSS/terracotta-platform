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
package org.terracotta.inet;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Objects;

import static org.terracotta.inet.HostAndIpValidator.isValidIPv6;

public class InetSocketAddressUtils {
  public static boolean areEqual(InetSocketAddress one, InetSocketAddress two) {
    return Objects.equals(encloseInBracketsIfIpv6(one), encloseInBracketsIfIpv6(two));
  }

  public static InetSocketAddress encloseInBracketsIfIpv6(InetSocketAddress address) {
    if (address != null && isValidIPv6(address.getHostString(), false)) {
      address = InetSocketAddress.createUnresolved("[" + address.getHostString() + "]", address.getPort());
    }
    return address;
  }

  public static boolean contains(Collection<InetSocketAddress> addresses, InetSocketAddress address) {
    for (InetSocketAddress addr : addresses) {
      if (areEqual(addr, address)) {
        return true;
      }
    }
    return false;
  }

  public static boolean containsAll(Collection<InetSocketAddress> addresses, Collection<InetSocketAddress> subset) {
    for (InetSocketAddress element : subset) {
      if (!contains(addresses, element)) {
        return false;
      }
    }
    return true;
  }
}
