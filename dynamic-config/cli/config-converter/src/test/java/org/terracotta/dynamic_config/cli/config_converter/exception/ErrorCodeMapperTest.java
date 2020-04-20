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

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.terracotta.dynamic_config.cli.config_converter.exception.ErrorCode.BOTH_WHITELIST_AND_DEPR_WHITELIST_PRESENT;
import static org.terracotta.dynamic_config.cli.config_converter.exception.ErrorCode.MISMATCHED_CLIENT_LEASE_DURATION;
import static org.terracotta.dynamic_config.cli.config_converter.exception.ErrorCode.MISMATCHED_DATA_DIRS;
import static org.terracotta.dynamic_config.cli.config_converter.exception.ErrorCode.MISMATCHED_DATA_DIR_NUMBERS;
import static org.terracotta.dynamic_config.cli.config_converter.exception.ErrorCode.MISMATCHED_OFFHEAP_RESOURCES;
import static org.terracotta.dynamic_config.cli.config_converter.exception.ErrorCode.MISMATCHED_OFFHEAP_RESOURCE_NUMBERS;
import static org.terracotta.dynamic_config.cli.config_converter.exception.ErrorCode.MISMATCHED_SECURITY_CONFIGURATION;
import static org.terracotta.dynamic_config.cli.config_converter.exception.ErrorCode.MULTIPLE_PLATFORM_DATA_DIRS;
import static org.terracotta.dynamic_config.cli.config_converter.exception.ErrorCode.NON_UNIQUE_PLATFORM_DATA_DIR_NAME;
import static org.terracotta.dynamic_config.cli.config_converter.exception.ErrorCode.PLATFORM_DATA_DIR_MISSING_IN_SOME_CONFIG_FILES;

public class ErrorCodeMapperTest {

  @Test
  public void testGetErrorCode() {
    ErrorCodeMapper.ErrorDetail errorDetail = ErrorCodeMapper.getErrorCode(800001L);
    assertThat(errorDetail.getErrorCode(), is(MISMATCHED_DATA_DIR_NUMBERS));

    errorDetail = ErrorCodeMapper.getErrorCode(800002L);
    assertThat(errorDetail.getErrorCode(), is(PLATFORM_DATA_DIR_MISSING_IN_SOME_CONFIG_FILES));
    errorDetail = ErrorCodeMapper.getErrorCode(800003L);
    assertThat(errorDetail.getErrorCode(), is(MISMATCHED_DATA_DIRS));
    errorDetail = ErrorCodeMapper.getErrorCode(800004L);
    assertThat(errorDetail.getErrorCode(), is(NON_UNIQUE_PLATFORM_DATA_DIR_NAME));
    errorDetail = ErrorCodeMapper.getErrorCode(800005L);
    assertThat(errorDetail.getErrorCode(), is(MULTIPLE_PLATFORM_DATA_DIRS));

    errorDetail = ErrorCodeMapper.getErrorCode(900001L);
    assertThat(errorDetail.getErrorCode(), is(BOTH_WHITELIST_AND_DEPR_WHITELIST_PRESENT));
    errorDetail = ErrorCodeMapper.getErrorCode(900002L);
    assertThat(errorDetail.getErrorCode(), is(MISMATCHED_SECURITY_CONFIGURATION));

    errorDetail = ErrorCodeMapper.getErrorCode(700001L);
    assertThat(errorDetail.getErrorCode(), is(MISMATCHED_CLIENT_LEASE_DURATION));

    errorDetail = ErrorCodeMapper.getErrorCode(500001L);
    assertThat(errorDetail.getErrorCode(), is(MISMATCHED_OFFHEAP_RESOURCE_NUMBERS));
    errorDetail = ErrorCodeMapper.getErrorCode(500002L);
    assertThat(errorDetail.getErrorCode(), is(MISMATCHED_OFFHEAP_RESOURCES));
  }
}
