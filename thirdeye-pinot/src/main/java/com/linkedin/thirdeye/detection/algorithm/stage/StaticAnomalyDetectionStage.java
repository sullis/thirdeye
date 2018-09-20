/*
 * Copyright (C) 2014-2018 LinkedIn Corp. (pinot-core@linkedin.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.thirdeye.detection.algorithm.stage;

import com.linkedin.thirdeye.datalayer.dto.MergedAnomalyResultDTO;
import com.linkedin.thirdeye.detection.DataProvider;
import com.linkedin.thirdeye.detection.InputData;
import com.linkedin.thirdeye.detection.InputDataSpec;
import java.util.List;

import static com.linkedin.thirdeye.detection.algorithm.stage.StageUtils.*;


/**
 * Static Anomaly detection stage. High level interface for anomaly detection stage.
 */
public abstract class StaticAnomalyDetectionStage implements AnomalyDetectionStage {

  /**
   * Returns a data spec describing all required data(time series, aggregates, existing anomalies) to perform a stage.
   * Data is retrieved in one pass and cached between executions if possible.
   * @return input data spec
   */
  abstract InputDataSpec getInputDataSpec();

  /**
   * Run detection in the specified time range and return a list of anomalies
   * @param data data(time series, anomalies, etc.) as described by data spec
   * @return list of anomalies
   */
  abstract List<MergedAnomalyResultDTO> runDetection(InputData data);

  @Override
  public final List<MergedAnomalyResultDTO> runDetection(DataProvider provider) {
    return this.runDetection(getDataForSpec(provider, this.getInputDataSpec()));
  }

}
