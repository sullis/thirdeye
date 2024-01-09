/*
 * Copyright 2024 StarTree Inc
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
package ai.startree.thirdeye.notification;

import static ai.startree.thirdeye.notification.SubscriptionGroupWatermarkManager.getStartTime;
import static ai.startree.thirdeye.notification.SubscriptionGroupWatermarkManager.newVectorClocks;
import static ai.startree.thirdeye.spi.util.AnomalyUtils.isIgnore;
import static ai.startree.thirdeye.spi.util.SpiUtils.optional;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toSet;

import ai.startree.thirdeye.spi.datalayer.AnomalyFilter;
import ai.startree.thirdeye.spi.datalayer.bao.AlertManager;
import ai.startree.thirdeye.spi.datalayer.bao.AnomalyManager;
import ai.startree.thirdeye.spi.datalayer.dto.AbstractDTO;
import ai.startree.thirdeye.spi.datalayer.dto.AlertAssociationDto;
import ai.startree.thirdeye.spi.datalayer.dto.AlertDTO;
import ai.startree.thirdeye.spi.datalayer.dto.AnomalyDTO;
import ai.startree.thirdeye.spi.datalayer.dto.SubscriptionGroupDTO;
import ai.startree.thirdeye.spi.detection.AnomalyResultSource;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Subscription Group filter collects all anomalies and returns back a Result
 */
@Singleton
public class SubscriptionGroupFilter {

  private static final Logger LOG = LoggerFactory.getLogger(SubscriptionGroupFilter.class);

  private static final String PROP_DETECTION_CONFIG_IDS = "detectionConfigIds";
  private static final Set<AnomalyResultSource> ANOMALY_RESULT_SOURCES = Set.of(
      AnomalyResultSource.DEFAULT_ANOMALY_DETECTION,
      AnomalyResultSource.ANOMALY_REPLAY
  );

  private final AnomalyManager anomalyManager;
  private final AlertManager alertManager;

  @Inject
  public SubscriptionGroupFilter(final AnomalyManager anomalyManager,
      final AlertManager alertManager) {
    this.anomalyManager = anomalyManager;
    this.alertManager = alertManager;
  }

  /**
   * Helper to determine presence of user-feedback for an anomaly
   *
   * @param anomaly anomaly
   * @return {@code true} if feedback exists and is TRUE or FALSE, {@code false} otherwise
   */
  private static boolean hasFeedback(final AnomalyDTO anomaly) {
    return anomaly.getFeedback() != null
        && !anomaly.getFeedback().getFeedbackType().isUnresolved();
  }

  private static boolean shouldFilter(final AnomalyDTO anomaly, final long startTime) {
    return anomaly != null
        && !anomaly.isChild()
        && !hasFeedback(anomaly)
        && anomaly.getCreateTime().getTime() > startTime
        && !isIgnore(anomaly)
        && ANOMALY_RESULT_SOURCES.contains(anomaly.getAnomalyResultSource());
  }

  private static AlertDTO fromId(final Long id) {
    final AlertDTO alert = new AlertDTO();
    alert.setId(id);
    return alert;
  }

  private static String toFormattedDate(long ts) {
    return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(ts));
  }

  /**
   * Returns {@code value} casted as List via {@code getList(Object value)} and parsed via {@code
   * getLongs(Collection&lt;Number&gt;)}.
   *
   * @param value value casted as list and parsed
   * @return equivalent collection of Long without nulls
   */
  private static List<Long> getLongs(Object value) {
    if (value == null) {
      return Collections.emptyList();
    }

    checkArgument(value instanceof Collection, "Expected Collection, got %s", value.getClass());
    return ((Collection<Number>) value).stream()
        .filter(Objects::nonNull)
        .map(Number::longValue)
        .collect(Collectors.toList());
  }

  /**
   * Find anomalies for the given subscription group given an end time.
   *
   * @param sg subscription group
   * @param endTime end time
   * @return set of anomalies
   */
  public Set<AnomalyDTO> filter(final SubscriptionGroupDTO sg, final long endTime) {
    final List<AlertAssociationDto> alertAssociations = optional(sg.getAlertAssociations())
        .orElseGet(() -> generate(sg));

    // Fetch all the anomalies to be notified to the recipients
    final Map<Long, Long> vectorClocks = newVectorClocks(alertAssociations, sg.getVectorClocks());
    return alertAssociations.stream()
        .filter(aa -> isAlertActive(aa.getAlert().getId()))
        .map(alertAssociation -> findAnomaliesForAlertAssociation(alertAssociation,
            sg.getId(),
            vectorClocks,
            endTime))
        .flatMap(Collection::stream)
        .collect(toSet());
  }

  /**
   * Generate List of Alert Association objects from existing subscription group
   *
   * @return List of Alert Association objects
   */
  private List<AlertAssociationDto> generate(final SubscriptionGroupDTO subscriptionGroup) {
    final List<Long> alertIds = getLongs(subscriptionGroup.getProperties()
        .get(PROP_DETECTION_CONFIG_IDS));

    return alertIds.stream()
        .map(SubscriptionGroupFilter::fromId)
        .map(alert -> new AlertAssociationDto().setAlert(alert))
        .collect(Collectors.toList());
  }

  private boolean isAlertActive(final long alertId) {
    final AlertDTO alert = alertManager.findById(alertId);
    return alert != null && alert.isActive();
  }

  private Set<AnomalyDTO> findAnomaliesForAlertAssociation(
      final AlertAssociationDto aa,
      final Long subscriptionGroupId,
      final Map<Long, Long> vectorClocks,
      final long endTime) {
    final long alertId = aa.getAlert().getId();
    final long startTime = getStartTime(vectorClocks, endTime, alertId);

    final AnomalyFilter anomalyFilter = new AnomalyFilter()
        .setCreateTimeWindow(new Interval(startTime + 1, endTime))
        .setAlertId(alertId);

    optional(aa.getEnumerationItem())
        .map(AbstractDTO::getId)
        .ifPresent(anomalyFilter::setEnumerationItemId);

    final Collection<AnomalyDTO> candidates = anomalyManager.filter(anomalyFilter);

    final Set<AnomalyDTO> anomaliesToBeNotified = candidates.stream()
        .filter(anomaly -> shouldFilter(anomaly, startTime))
        .collect(toSet());

    LOG.info("Subscription Group: {} Alert: {}. "
            + "Found {} out of {} anomalies to be notified from {} to {} ({} to {} System Time)",
        subscriptionGroupId,
        alertId,
        anomaliesToBeNotified.size(),
        candidates.size(),
        startTime,
        endTime,
        toFormattedDate(startTime),
        toFormattedDate(endTime));

    return anomaliesToBeNotified;
  }
}
