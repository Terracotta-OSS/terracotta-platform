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
package org.terracotta.dynamic_config.cli.config_converter.exception;

public enum ErrorCode {
  UNKNOWN_ERROR(10),
  INVALID_INPUT_PATTERN(50),
  INVALID_MIXED_INPUT_PATTERN(75),
  SAME_SERVICE_DEFINED_MULTIPLE_TIMES(100),
  MISMATCHED_SERVICE_CONFIGURATION(200),
  DUPLICATE_STRIPE_NAME(300),
  DUPLICATE_SERVER_NAME_IN_STRIPE(460),
  INVALID_FILE_TYPE(600),
  MISSING_SERVERS(700),
  MISMATCHED_SERVERS(800),
  INVALID_ATTRIBUTE_NAME(900),
  MISMATCHED_OFFHEAP_RESOURCES(1000),
  MISMATCHED_OFFHEAP_RESOURCE_NUMBERS(1050),
  BOTH_WHITELIST_AND_DEPR_WHITELIST_PRESENT(1100),
  MISMATCHED_SECURITY_CONFIGURATION(1200),
  MISMATCHED_CLIENT_LEASE_DURATION(1300),
  MISMATCHED_DATA_DIR_NUMBERS(1400),
  MISMATCHED_DATA_DIRS(1500),
  MULTIPLE_PLATFORM_DATA_DIRS(1600),
  NON_UNIQUE_PLATFORM_DATA_DIR_NAME(1700),
  PLATFORM_DATA_DIR_MISSING_IN_SOME_CONFIG_FILES(1800),
  NO_DATA_DIR_WITH_PLATFORM_PERSISTENCE(1900),
  INVALID_DATA_DIR_FOR_PLATFORM_PERSISTENCE(2000),
  UNEXPECTED_ERROR_FROM_NOMAD_PREPARE_PHASE(60000);

  private final int code;

  ErrorCode(int errorCode) {
    this.code = errorCode;
  }
}