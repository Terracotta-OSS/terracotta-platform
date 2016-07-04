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
package org.terracotta.management.service.impl;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

/**
 * Constants used through out the management service.
 * <p>
 * TODO: This will eventually move as a part of management service configuration
 * once the POC is done.
 *
 * @author RKAV
 */
public class Constants {
  // size of each partition buffer
  public static final int BUFFER_CACHE_SIZE = 1 << 13;
  // how much overspill to store before discarding in case the management entity is down
  public static final int OVERSPILL_SIZE = 1 << 10;
  // default interval for consuming incoming messages
  public static final int COLLECTION_INTERVAL = 1000;
  public static final TimeUnit DEFAULT_TIME_UNIT = MILLISECONDS;

  // number of parallel producers that 'pushes' messages to management service
  public static final int MAX_PARALLEL_PRODUCERS = 10;
}
