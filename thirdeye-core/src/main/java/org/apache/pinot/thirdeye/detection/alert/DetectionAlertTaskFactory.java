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

package org.apache.pinot.thirdeye.detection.alert;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.pinot.thirdeye.config.ThirdEyeWorkerConfiguration;
import org.apache.pinot.thirdeye.datalayer.bao.AlertManager;
import org.apache.pinot.thirdeye.datalayer.bao.EventManager;
import org.apache.pinot.thirdeye.datalayer.bao.MergedAnomalyResultManager;
import org.apache.pinot.thirdeye.datalayer.bao.MetricConfigManager;
import org.apache.pinot.thirdeye.datalayer.dto.SubscriptionGroupDTO;
import org.apache.pinot.thirdeye.detection.ConfigUtils;
import org.apache.pinot.thirdeye.detection.DataProvider;
import org.apache.pinot.thirdeye.detection.alert.scheme.DetectionAlertScheme;
import org.apache.pinot.thirdeye.detection.alert.suppress.DetectionAlertSuppressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DetectionAlertTaskFactory {

  private static final Logger LOG = LoggerFactory.getLogger(DetectionAlertTaskFactory.class);

  private static final String PROP_CLASS_NAME = "className";
  private static final String PROP_EMAIL_SCHEME = "emailScheme";
  private static final String DEFAULT_ALERT_SCHEME = "org.apache.pinot.thirdeye.detection.alert.scheme.DetectionEmailAlerter";

  private final DataProvider provider;
  private final MergedAnomalyResultManager mergedAnomalyResultManager;
  private final AlertManager alertManager;
  private final MetricConfigManager metricConfigManager;
  private final EventManager eventManager;

  @Inject
  public DetectionAlertTaskFactory(final DataProvider provider,
      final MergedAnomalyResultManager mergedAnomalyResultManager,
      final AlertManager alertManager,
      final MetricConfigManager metricConfigManager,
      final EventManager eventManager) {
    this.provider = provider;
    this.mergedAnomalyResultManager = mergedAnomalyResultManager;
    this.alertManager = alertManager;
    this.metricConfigManager = metricConfigManager;
    this.eventManager = eventManager;
  }

  public DetectionAlertFilter loadAlertFilter(SubscriptionGroupDTO alertConfig, long endTime)
      throws Exception {
    Preconditions.checkNotNull(alertConfig);
    String className = alertConfig.getProperties().get(PROP_CLASS_NAME).toString();
    LOG.debug("Loading Alert Filter : {}", className);
    Constructor<?> constructor = Class.forName(className)
        .getConstructor(DataProvider.class,
            SubscriptionGroupDTO.class,
            long.class,
            MergedAnomalyResultManager.class,
            AlertManager.class);
    return (DetectionAlertFilter) constructor.newInstance(provider,
        alertConfig,
        endTime,
        this.mergedAnomalyResultManager,
        this.alertManager);
  }

  public Set<DetectionAlertScheme> loadAlertSchemes(SubscriptionGroupDTO alertConfig,
      ThirdEyeWorkerConfiguration thirdeyeConfig, DetectionAlertFilterResult result)
      throws Exception {
    Preconditions.checkNotNull(alertConfig);
    Map<String, Object> alertSchemes = alertConfig.getAlertSchemes();
    if (alertSchemes == null || alertSchemes.isEmpty()) {
      Map<String, Object> emailScheme = new HashMap<>();
      emailScheme.put(PROP_CLASS_NAME, DEFAULT_ALERT_SCHEME);
      alertSchemes = Collections.singletonMap(PROP_EMAIL_SCHEME, emailScheme);
    }
    Set<DetectionAlertScheme> detectionAlertSchemeSet = new HashSet<>();
    for (String alertSchemeType : alertSchemes.keySet()) {
      LOG.debug("Loading Alert Scheme : {}", alertSchemeType);
      Preconditions.checkNotNull(alertSchemes.get(alertSchemeType));
      Preconditions
          .checkNotNull(ConfigUtils.getMap(alertSchemes.get(alertSchemeType)).get(PROP_CLASS_NAME));
      Constructor<?> constructor = Class.forName(ConfigUtils.getMap(alertSchemes.get(alertSchemeType))
          .get(PROP_CLASS_NAME).toString().trim())
          .getConstructor(SubscriptionGroupDTO.class,
              ThirdEyeWorkerConfiguration.class,
              DetectionAlertFilterResult.class,
              MetricConfigManager.class,
              AlertManager.class,
              EventManager.class,
              MergedAnomalyResultManager.class);

      detectionAlertSchemeSet
          .add((DetectionAlertScheme) constructor.newInstance(alertConfig,
              thirdeyeConfig,
              result,
              this.metricConfigManager,
              this.alertManager,
              this.eventManager,
              this.mergedAnomalyResultManager));
    }
    return detectionAlertSchemeSet;
  }

  public Set<DetectionAlertSuppressor> loadAlertSuppressors(SubscriptionGroupDTO alertConfig)
      throws Exception {
    Preconditions.checkNotNull(alertConfig);
    Set<DetectionAlertSuppressor> detectionAlertSuppressors = new HashSet<>();
    Map<String, Object> alertSuppressors = alertConfig.getAlertSuppressors();
    if (alertSuppressors == null || alertSuppressors.isEmpty()) {
      return detectionAlertSuppressors;
    }

    for (String alertSuppressor : alertSuppressors.keySet()) {
      LOG.debug("Loading Alert Suppressor : {}", alertSuppressor);
      Preconditions.checkNotNull(alertSuppressors.get(alertSuppressor));
      Preconditions.checkNotNull(
          ConfigUtils.getMap(alertSuppressors.get(alertSuppressor)).get(PROP_CLASS_NAME));
      Constructor<?> constructor = Class
          .forName(ConfigUtils.getMap(alertSuppressors.get(alertSuppressor))
              .get(PROP_CLASS_NAME).toString().trim())
          .getConstructor(SubscriptionGroupDTO.class, MergedAnomalyResultManager.class);
      detectionAlertSuppressors
          .add((DetectionAlertSuppressor) constructor.newInstance(alertConfig,
              this.mergedAnomalyResultManager));
    }

    return detectionAlertSuppressors;
  }
}
