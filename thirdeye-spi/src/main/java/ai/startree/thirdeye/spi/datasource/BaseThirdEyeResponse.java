/*
 * Copyright (c) 2022 StarTree Inc. All rights reserved.
 * Confidential and Proprietary Information of StarTree Inc.
 */

package ai.startree.thirdeye.spi.datasource;

import ai.startree.thirdeye.spi.detection.TimeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class BaseThirdEyeResponse implements ThirdEyeResponse {

  protected final List<MetricFunction> metricFunctions;
  protected final ThirdEyeRequest request;
  protected final TimeSpec dataTimeSpec;
  protected final List<String> groupKeyColumns;
  protected final String[] allColumnNames;

  public BaseThirdEyeResponse(ThirdEyeRequest request, TimeSpec dataTimeSpec) {
    this.request = request;
    this.dataTimeSpec = dataTimeSpec;
    // todo cyril remove the list
    this.metricFunctions = List.of(request.getMetricFunction());
    this.groupKeyColumns = new ArrayList<>();
    if (request.getGroupByTimeGranularity() != null) {
      groupKeyColumns.add(dataTimeSpec.getColumnName());
    }
    groupKeyColumns.addAll(request.getGroupBy());
    ArrayList<String> allColumnNameList = new ArrayList<>();
    allColumnNameList.addAll(request.getGroupBy());
    for (MetricFunction function : List.of(request.getMetricFunction())) {
      allColumnNameList.add(function.toString());
    }
    allColumnNames = new String[allColumnNameList.size()];
    allColumnNameList.toArray(allColumnNames);
  }

  @Override
  public List<MetricFunction> getMetricFunctions() {
    return metricFunctions;
  }

  @Override
  public abstract int getNumRows();

  @Override
  public abstract ThirdEyeResponseRow getRow(int rowId);

  @Override
  public abstract int getNumRowsFor(MetricFunction metricFunction);

  @Override
  public abstract Map<String, String> getRow(MetricFunction metricFunction, int rowId);

  @Override
  public ThirdEyeRequest getRequest() {
    return request;
  }

  @Override
  public TimeSpec getDataTimeSpec() {
    return dataTimeSpec;
  }

  @Override
  public List<String> getGroupKeyColumns() {
    return groupKeyColumns;
  }

  public String[] getAllColumnNames() {
    return allColumnNames;
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
