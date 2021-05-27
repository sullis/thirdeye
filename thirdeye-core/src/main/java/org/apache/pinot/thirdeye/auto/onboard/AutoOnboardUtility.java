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

package org.apache.pinot.thirdeye.auto.onboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.pinot.thirdeye.datasource.DataSourceConfig;
import org.apache.pinot.thirdeye.datasource.DataSourcesConfiguration;
import org.apache.pinot.thirdeye.spi.auto.onboard.AutoOnboard;
import org.apache.pinot.thirdeye.spi.datasource.MetadataSourceConfig;
import org.apache.pinot.thirdeye.spi.datasource.ThirdEyeDataSourceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoOnboardUtility {

  private static final Logger LOG = LoggerFactory.getLogger(AutoOnboardUtility.class);

  public static Map<String, List<AutoOnboard>> getDataSourceToAutoOnboardMap(
      final DataSourcesConfiguration dataSourcesConfiguration,
      final ThirdEyeDataSourceContext context) {
    final Map<String, List<AutoOnboard>> dataSourceToOnboardMap = new HashMap<>();

    for (DataSourceConfig dataSourceConfig : dataSourcesConfiguration.getDataSourceConfigs()) {
      processDataSourceConfig(dataSourceToOnboardMap,
          dataSourceConfig,
          context);
    }

    return dataSourceToOnboardMap;
  }

  private static void processDataSourceConfig(
      final Map<String, List<AutoOnboard>> dataSourceToOnboardMap,
      final DataSourceConfig dataSourceConfig,
      final ThirdEyeDataSourceContext context) {
    final List<MetadataSourceConfig> metadataSourceConfigs = dataSourceConfig
        .getMetadataSourceConfigs();
    if (metadataSourceConfigs == null) {
      return;
    }

    for (MetadataSourceConfig metadataSourceConfig : metadataSourceConfigs) {
      String metadataSourceClassName = metadataSourceConfig.getClassName();
      // Inherit properties from Data Source
      metadataSourceConfig.getProperties().putAll(dataSourceConfig.getProperties());
      if (StringUtils.isNotBlank(metadataSourceClassName)) {
        try {
          final AutoOnboard instance = createAutoOnboardInstance(context,
              metadataSourceConfig,
              metadataSourceClassName);
          String datasourceClassName = dataSourceConfig.getClassName();
          String dataSource = datasourceClassName.substring(
              datasourceClassName.lastIndexOf(".") + 1);

          dataSourceToOnboardMap
              .computeIfAbsent(dataSource, k -> new ArrayList<>())
              .add(instance);
        } catch (Exception e) {
          LOG.error("Exception in creating metadata constructor {}", metadataSourceClassName, e);
        }
      }
    }
  }

  private static AutoOnboard createAutoOnboardInstance(final ThirdEyeDataSourceContext context,
      final MetadataSourceConfig metadataSourceConfig, final String metadataSourceClassName)
      throws ReflectiveOperationException {
    final AutoOnboard instance = (AutoOnboard) Class
        .forName(metadataSourceClassName)
        .getConstructor(MetadataSourceConfig.class)
        .newInstance(metadataSourceConfig);
    instance.init(context);
    return instance;
  }
}
