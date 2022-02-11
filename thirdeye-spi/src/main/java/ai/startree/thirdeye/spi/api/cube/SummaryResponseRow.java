/*
 * Copyright (c) 2022 StarTree Inc. All rights reserved.
 * Confidential and Proprietary Information of StarTree Inc.
 */

package ai.startree.thirdeye.spi.api.cube;

import java.util.List;

/**
 * A POJO for front-end representation.
 */
public class SummaryResponseRow extends BaseResponseRow {

  private List<String> names;
  private List<String> otherDimensionValues;
  private int moreOtherDimensionNumber;
  private double cost;

  public List<String> getNames() {
    return names;
  }

  public SummaryResponseRow setNames(final List<String> names) {
    this.names = names;
    return this;
  }

  public List<String> getOtherDimensionValues() {
    return otherDimensionValues;
  }

  public SummaryResponseRow setOtherDimensionValues(final List<String> otherDimensionValues) {
    this.otherDimensionValues = otherDimensionValues;
    return this;
  }

  public int getMoreOtherDimensionNumber() {
    return moreOtherDimensionNumber;
  }

  public SummaryResponseRow setMoreOtherDimensionNumber(final int moreOtherDimensionNumber) {
    this.moreOtherDimensionNumber = moreOtherDimensionNumber;
    return this;
  }

  public double getCost() {
    return cost;
  }

  public SummaryResponseRow setCost(final double cost) {
    this.cost = cost;
    return this;
  }
}