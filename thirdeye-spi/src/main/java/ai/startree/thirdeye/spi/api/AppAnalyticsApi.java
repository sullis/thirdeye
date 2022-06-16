/*
 * Copyright (c) 2022 StarTree Inc. All rights reserved.
 * Confidential and Proprietary Information of StarTree Inc.
 */

package ai.startree.thirdeye.spi.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class AppAnalyticsApi implements ThirdEyeApi {

  private String version;

  public String getVersion() {
    return version;
  }

  public AppAnalyticsApi setVersion(final String version) {
    this.version = version;
    return this;
  }
}
