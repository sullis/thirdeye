/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.pinot.thirdeye.detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.pinot.thirdeye.datalayer.bao.TestDbEnv;
import org.apache.pinot.thirdeye.detection.annotation.registry.DetectionRegistry;
import org.apache.pinot.thirdeye.detection.anomaly.task.TaskContext;
import org.apache.pinot.thirdeye.spi.datalayer.bao.AlertManager;
import org.apache.pinot.thirdeye.spi.datalayer.bao.EvaluationManager;
import org.apache.pinot.thirdeye.spi.datalayer.bao.MergedAnomalyResultManager;
import org.apache.pinot.thirdeye.spi.datalayer.dto.AlertDTO;
import org.apache.pinot.thirdeye.spi.datalayer.dto.MergedAnomalyResultDTO;
import org.apache.pinot.thirdeye.spi.detection.DataProvider;
import org.apache.pinot.thirdeye.spi.detection.DetectionPipelineTaskInfo;
import org.apache.pinot.thirdeye.spi.detection.dimension.DimensionMap;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DetectionPipelineTaskRunnerTest {

  private List<MockPipeline> runs;
  private List<MockPipelineOutput> outputs;

  private DetectionPipelineTaskRunner runner;
  private DetectionPipelineTaskInfo info;
  private TaskContext context;

  private TestDbEnv testDAOProvider;
  private AlertManager detectionDAO;
  private MergedAnomalyResultManager anomalyDAO;
  private EvaluationManager evaluationDAO;
  private DetectionPipelineFactory loader;
  private DataProvider provider;
  private Map<String, Object> properties;

  private long detectorId;

  @BeforeMethod
  public void beforeMethod() {
    this.runs = new ArrayList<>();

    this.outputs = new ArrayList<>();

    this.testDAOProvider = new TestDbEnv();
    this.detectionDAO = TestDbEnv.getInstance().getDetectionConfigManager();
    this.anomalyDAO = TestDbEnv.getInstance().getMergedAnomalyResultDAO();
    this.evaluationDAO = TestDbEnv.getInstance().getEvaluationManager();
    this.provider = new MockDataProvider();
    this.loader = new MockPipelineLoader(this.runs, this.outputs, provider);

    this.properties = new HashMap<>();
    this.properties.put("metricUrn", "thirdeye:metric:1");
    this.properties.put("className", "myClassName");

    AlertDTO detector = new AlertDTO();
    detector.setProperties(this.properties);
    detector.setName("myName");
    detector.setDescription("myDescription");
    detector.setCron("myCron");
    this.detectorId = this.detectionDAO.save(detector);

    this.runner = new DetectionPipelineTaskRunner(
        this.detectionDAO,
        this.anomalyDAO,
        this.evaluationDAO,
        this.loader,
        new ModelRetuneFlow(this.provider, new DetectionRegistry()),
        TestDbEnv.getInstance().getAnomalySubscriptionGroupNotificationManager()
    );

    this.info = new DetectionPipelineTaskInfo();
    this.info.setConfigId(this.detectorId);
    this.info.setStart(1250);
    this.info.setEnd(1500);

    this.context = new TaskContext();
  }

  @AfterMethod(alwaysRun = true)
  public void afterMethod() {
    this.testDAOProvider.cleanup();
  }

  @Test
  public void testTaskRunnerLoading() throws Exception {
    this.runner.execute(this.info, this.context);

    Assert.assertEquals(this.runs.size(), 1);
    Assert.assertEquals(this.runs.get(0).getStartTime(), 1250);
    Assert.assertEquals(this.runs.get(0).getEndTime(), 1500);
    Assert.assertEquals(this.runs.get(0).getConfig().getName(), "myName");
    Assert.assertEquals(this.runs.get(0).getConfig().getDescription(), "myDescription");
    Assert
        .assertEquals(this.runs.get(0).getConfig().getProperties().get("className"), "myClassName");
    Assert.assertEquals(this.runs.get(0).getConfig().getCron(), "myCron");
  }

  @Test
  public void testTaskRunnerPersistence() throws Exception {
    MergedAnomalyResultDTO anomaly = DetectionTestUtils
        .makeAnomaly(this.detectorId, 1300, 1400, null, null,
            Collections.singletonMap("myKey", "myValue"));

    this.outputs.add(new MockPipelineOutput(Collections.singletonList(anomaly), 1400));

    this.runner.execute(this.info, this.context);

    Assert.assertNotNull(anomaly.getId());

    MergedAnomalyResultDTO readAnomaly = this.anomalyDAO.findById(anomaly.getId());
    Assert.assertEquals(readAnomaly.getDetectionConfigId(), Long.valueOf(this.detectorId));
    Assert.assertEquals(readAnomaly.getStartTime(), 1300);
    Assert.assertEquals(readAnomaly.getEndTime(), 1400);
    Assert.assertEquals(readAnomaly.getDimensions(), new DimensionMap("{\"myKey\":\"myValue\"}"));
  }

  @Test
  public void testTaskRunnerPersistenceFailTimestamp() throws Exception {
    MergedAnomalyResultDTO anomaly = DetectionTestUtils
        .makeAnomaly(this.detectorId, 1300, 1400, null, null,
            Collections.singletonMap("myKey", "myValue"));

    this.outputs.add(new MockPipelineOutput(Collections.singletonList(anomaly), -1));

    this.runner.execute(this.info, this.context);

    Assert.assertNull(anomaly.getId());
  }
}
