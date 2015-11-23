package com.linkedin.thirdeye;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

public class ThirdEyeDetectorConfiguration extends Configuration {
  @Valid
  @NotNull
  private String thirdEyeHost;

  @Valid
  @NotNull
  private int thirdEyePort;

  @Valid
  @NotNull
  private String functionConfigPath;

  @Valid
  @NotNull
  private final DataSourceFactory database = new DataSourceFactory();

  @JsonProperty("database")
  public DataSourceFactory getDatabase() {
    return database;
  }

  public int getThirdEyePort() {
    return thirdEyePort;
  }

  public void setThirdEyePort(int thirdEyePort) {
    this.thirdEyePort = thirdEyePort;
  }

  public String getThirdEyeHost() {
    return thirdEyeHost;
  }

  public void setThirdEyeHost(String thirdEyeHost) {
    this.thirdEyeHost = thirdEyeHost;
  }

  public String getFunctionConfigPath() {
    return functionConfigPath;
  }

  public void setFunctionConfigPath(String functionConfigPath) {
    this.functionConfigPath = functionConfigPath;
  }
}
