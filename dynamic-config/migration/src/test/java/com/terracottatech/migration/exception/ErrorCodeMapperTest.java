/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration.exception;

import org.junit.Test;

import static com.terracottatech.migration.exception.ErrorCode.BOTH_WHITELIST_DEPR_WHITELIST_PRESENT;
import static com.terracottatech.migration.exception.ErrorCode.MISMATCHED_DATA_DIR;
import static com.terracottatech.migration.exception.ErrorCode.MISMATCHED_DATA_DIR_RESOURCE_NUMBERS;
import static com.terracottatech.migration.exception.ErrorCode.MISMATCHED_LEASE_TIME_CONFIGURATION;
import static com.terracottatech.migration.exception.ErrorCode.MISMATCHED_OFF_HEAPS;
import static com.terracottatech.migration.exception.ErrorCode.MISMATCHED_OFF_HEAP_RESOURCE_NUMBERS;
import static com.terracottatech.migration.exception.ErrorCode.MISMATCHED_SECURITY_CONFIGURATION;
import static com.terracottatech.migration.exception.ErrorCode.MULTIPLE_PLATFORM_DATA_DIRS;
import static com.terracottatech.migration.exception.ErrorCode.NON_UNIQUE_PLATFORM_DATA_ROOT_NAME;
import static com.terracottatech.migration.exception.ErrorCode.PLATFORM_DATA_ROOT_MISSING_IN_SOME_OF_THE_CONFIG_FILES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ErrorCodeMapperTest {

  @Test
  public void testGetErrorCode() {
    ErrorCodeMapper.ErrorDetail errorDetail = ErrorCodeMapper.getErrorCode(800001L);
    assertThat(errorDetail.getErrorCode(), is(MISMATCHED_DATA_DIR_RESOURCE_NUMBERS));

    errorDetail = ErrorCodeMapper.getErrorCode(800002L);
    assertThat(errorDetail.getErrorCode(), is(PLATFORM_DATA_ROOT_MISSING_IN_SOME_OF_THE_CONFIG_FILES));
    errorDetail = ErrorCodeMapper.getErrorCode(800003L);
    assertThat(errorDetail.getErrorCode(), is(MISMATCHED_DATA_DIR));
    errorDetail = ErrorCodeMapper.getErrorCode(800004L);
    assertThat(errorDetail.getErrorCode(), is(NON_UNIQUE_PLATFORM_DATA_ROOT_NAME));
    errorDetail = ErrorCodeMapper.getErrorCode(800005L);
    assertThat(errorDetail.getErrorCode(), is(MULTIPLE_PLATFORM_DATA_DIRS));

    errorDetail = ErrorCodeMapper.getErrorCode(900001L);
    assertThat(errorDetail.getErrorCode(), is(BOTH_WHITELIST_DEPR_WHITELIST_PRESENT));
    errorDetail = ErrorCodeMapper.getErrorCode(900002L);
    assertThat(errorDetail.getErrorCode(), is(MISMATCHED_SECURITY_CONFIGURATION));

    errorDetail = ErrorCodeMapper.getErrorCode(700001L);
    assertThat(errorDetail.getErrorCode(), is(MISMATCHED_LEASE_TIME_CONFIGURATION));

    errorDetail = ErrorCodeMapper.getErrorCode(500001L);
    assertThat(errorDetail.getErrorCode(), is(MISMATCHED_OFF_HEAP_RESOURCE_NUMBERS));
    errorDetail = ErrorCodeMapper.getErrorCode(500002L);
    assertThat(errorDetail.getErrorCode(), is(MISMATCHED_OFF_HEAPS));
  }
}
