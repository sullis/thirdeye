/*
 * Copyright 2022 StarTree Inc
 *
 * Licensed under the StarTree Community License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.startree.ai/legal/startree-community-license
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT * WARRANTIES OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing permissions and limitations under
 * the License.
 */
package ai.startree.thirdeye.alert;

import static ai.startree.thirdeye.detectionpipeline.operator.ForkJoinOperator.K_COMBINER;
import static ai.startree.thirdeye.detectionpipeline.operator.ForkJoinOperator.K_ENUMERATOR;
import static ai.startree.thirdeye.detectionpipeline.operator.ForkJoinOperator.K_ROOT;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

import ai.startree.thirdeye.datasource.cache.DataSourceCache;
import ai.startree.thirdeye.detection.annotation.registry.DetectionRegistry;
import ai.startree.thirdeye.detectionpipeline.operator.CombinerOperator;
import ai.startree.thirdeye.detectionpipeline.operator.CombinerOperator.CombinerResult;
import ai.startree.thirdeye.detectionpipeline.operator.EchoOperator;
import ai.startree.thirdeye.detectionpipeline.operator.EchoOperator.EchoResult;
import ai.startree.thirdeye.detectionpipeline.plan.CombinerPlanNode;
import ai.startree.thirdeye.detectionpipeline.plan.EchoPlanNode;
import ai.startree.thirdeye.detectionpipeline.plan.EnumeratorPlanNode;
import ai.startree.thirdeye.detectionpipeline.plan.ForkJoinPlanNode;
import ai.startree.thirdeye.detectionpipeline.plan.PlanNodeFactory;
import ai.startree.thirdeye.spi.datalayer.dto.PlanNodeBean;
import ai.startree.thirdeye.spi.detection.v2.DetectionPipelineResult;
import ai.startree.thirdeye.spi.detection.v2.PlanNode;
import ai.startree.thirdeye.spi.detection.v2.PlanNodeContext;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PlanExecutorTest {

  private PlanExecutor planExecutor;

  @BeforeMethod
  public void setUp() {
    final PlanNodeFactory planNodeFactory = new PlanNodeFactory(
        mock(DataSourceCache.class),
        mock(DetectionRegistry.class));
    planExecutor = new PlanExecutor(planNodeFactory);
  }

  @Test
  public void testExecutePlanNode() throws Exception {
    final EchoPlanNode node = new EchoPlanNode();
    final String echoInput = "test_input";
    final String nodeName = "root";
    node.init(new PlanNodeContext()
        .setName(nodeName)
        .setDetectionInterval(new Interval(0L, 0L, DateTimeZone.UTC))
        .setPlanNodeBean(new PlanNodeBean()
            .setInputs(Collections.emptyList())
            .setParams(ImmutableMap.of(EchoOperator.DEFAULT_INPUT_KEY, echoInput))
        )
    );
    final HashMap<ContextKey, DetectionPipelineResult> context = new HashMap<>();
    final HashMap<String, PlanNode> pipelinePlanNodes = new HashMap<>();
    PlanExecutor.executePlanNode(
        pipelinePlanNodes,
        context,
        node
    );

    assertThat(context.size()).isEqualTo(1);
    final ContextKey key = PlanExecutor.key(nodeName, EchoOperator.DEFAULT_OUTPUT_KEY);
    final DetectionPipelineResult result = context.get(key);
    assertThat(result).isNotNull();

    final EchoResult echoResult = (EchoResult) result;
    assertThat(echoResult.text()).isEqualTo(echoInput);
  }

  @Test
  public void testExecuteSingleForkJoin() throws Exception {
    final PlanNodeBean echoNode = new PlanNodeBean()
        .setName("echo")
        .setType(EchoPlanNode.TYPE)
        .setParams(ImmutableMap.of(
            EchoOperator.DEFAULT_INPUT_KEY, "${key}"
        ));

    final PlanNodeBean enumeratorNode = new PlanNodeBean()
        .setName("enumerator")
        .setType(EnumeratorPlanNode.TYPE);

    final PlanNodeBean combinerNode = new PlanNodeBean()
        .setName("combiner")
        .setType(CombinerPlanNode.TYPE);

    final PlanNodeBean forkJoinNode = new PlanNodeBean()
        .setName("root")
        .setType(ForkJoinPlanNode.TYPE)
        .setParams(ImmutableMap.of(
            K_ENUMERATOR, enumeratorNode.getName(),
            K_ROOT, echoNode.getName(),
            K_COMBINER, combinerNode.getName()
        ));

    final List<PlanNodeBean> planNodeBeans = Arrays.asList(
        echoNode,
        enumeratorNode,
        combinerNode,
        forkJoinNode
    );

    final Map<ContextKey, DetectionPipelineResult> context = new HashMap<>();
    final Map<String, PlanNode> pipelinePlanNodes = planExecutor.buildPlanNodeMap(planNodeBeans,
        new Interval(0L, System.currentTimeMillis(), DateTimeZone.UTC));
    PlanExecutor.executePlanNode(
        pipelinePlanNodes,
        context,
        pipelinePlanNodes.get("root")
    );

    assertThat(context.size()).isEqualTo(1);

    final DetectionPipelineResult detectionPipelineResult = context.get(PlanExecutor.key("root",
        CombinerOperator.DEFAULT_OUTPUT_KEY));

    assertThat(detectionPipelineResult).isInstanceOf(CombinerResult.class);

    final CombinerResult combinerResult = (CombinerResult) detectionPipelineResult;
    final Map<String, DetectionPipelineResult> outputMap = combinerResult.getResults();

    assertThat(outputMap).isNotNull();

    assertThat(outputMap.values().size()).isEqualTo(3);

    final Set<String> strings = outputMap.values().stream()
        .map(r -> (EchoResult) r)
        .map(EchoResult::text)
        .collect(toSet());

    assertThat(strings).isEqualTo(Set.of("1", "2", "3"));
  }
}
