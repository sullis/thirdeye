package org.apache.pinot.thirdeye.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import org.apache.pinot.thirdeye.config.HolidayEventsLoaderConfiguration;
import org.apache.pinot.thirdeye.config.ThirdEyeSchedulerConfiguration;
import org.apache.pinot.thirdeye.datasource.AutoOnboardConfiguration;
import org.apache.pinot.thirdeye.datasource.AutoOnboardService;
import org.apache.pinot.thirdeye.detection.anomaly.detection.trigger.DataAvailabilityEventListenerDriver;
import org.apache.pinot.thirdeye.detection.anomaly.detection.trigger.DataAvailabilityTaskScheduler;
import org.apache.pinot.thirdeye.detection.anomaly.events.HolidayEventsLoader;
import org.apache.pinot.thirdeye.detection.anomaly.monitor.MonitorJobScheduler;
import org.apache.pinot.thirdeye.detection.download.ModelDownloaderManager;

@Singleton
public class SchedulerService implements Managed {

  private final ThirdEyeSchedulerConfiguration config;
  private final HolidayEventsLoaderConfiguration holidayEventsLoaderConfiguration;
  private final AutoOnboardConfiguration autoOnboardConfiguration;
  private final MonitorJobScheduler monitorJobScheduler;
  private final AutoOnboardService autoOnboardService;
  private final HolidayEventsLoader holidayEventsLoader;
  private final DetectionCronScheduler detectionScheduler;
  private final DataAvailabilityEventListenerDriver dataAvailabilityEventListenerDriver;
  private final ModelDownloaderManager modelDownloaderManager;
  private final DataAvailabilityTaskScheduler dataAvailabilityTaskScheduler;
  private final SubscriptionCronScheduler subscriptionScheduler;

  @Inject
  public SchedulerService(final ThirdEyeSchedulerConfiguration config,
      final HolidayEventsLoaderConfiguration holidayEventsLoaderConfiguration,
      final AutoOnboardConfiguration autoOnboardConfiguration,
      final MonitorJobScheduler monitorJobScheduler,
      final AutoOnboardService autoOnboardService,
      final HolidayEventsLoader holidayEventsLoader,
      final DetectionCronScheduler detectionScheduler,
      final DataAvailabilityEventListenerDriver dataAvailabilityEventListenerDriver,
      final ModelDownloaderManager modelDownloaderManager,
      final DataAvailabilityTaskScheduler dataAvailabilityTaskScheduler,
      final SubscriptionCronScheduler subscriptionScheduler) {
    this.config = config;
    this.holidayEventsLoaderConfiguration = holidayEventsLoaderConfiguration;
    this.autoOnboardConfiguration = autoOnboardConfiguration;
    this.monitorJobScheduler = monitorJobScheduler;
    this.autoOnboardService = autoOnboardService;
    this.holidayEventsLoader = holidayEventsLoader;
    this.detectionScheduler = detectionScheduler;
    this.dataAvailabilityEventListenerDriver = dataAvailabilityEventListenerDriver;
    this.modelDownloaderManager = modelDownloaderManager;
    this.dataAvailabilityTaskScheduler = dataAvailabilityTaskScheduler;
    this.subscriptionScheduler = subscriptionScheduler;
  }

  @Override
  public void start() throws Exception {
    if (config.isMonitor()) {
      monitorJobScheduler.start();
    }

    if (autoOnboardConfiguration.isEnabled()) {
      autoOnboardService.start();
    }

    if (holidayEventsLoaderConfiguration.isEnabled()) {
      holidayEventsLoader.start();
    }
    if (config.isDetectionPipeline()) {
      detectionScheduler.start();
    }
    if (config.isDetectionAlert()) {
      subscriptionScheduler.start();
    }
    if (config.isDataAvailabilityEventListener()) {
      dataAvailabilityEventListenerDriver.start();
    }
    if (config.isDataAvailabilityTaskScheduler()) {
      dataAvailabilityTaskScheduler.start();
    }
    if (config.getModelDownloaderConfigs() != null) {
      modelDownloaderManager.start();
    }
  }

  @Override
  public void stop() throws Exception {
    if (monitorJobScheduler != null) {
      monitorJobScheduler.shutdown();
    }
    if (holidayEventsLoaderConfiguration.isEnabled()) {
      holidayEventsLoader.shutdown();
    }
    if (autoOnboardConfiguration.isEnabled()) {
      autoOnboardService.shutdown();
    }
    if (detectionScheduler != null) {
      detectionScheduler.shutdown();
    }
    if (config.isDetectionAlert()) {
      subscriptionScheduler.shutdown();
    }
    if (dataAvailabilityEventListenerDriver != null) {
      dataAvailabilityEventListenerDriver.shutdown();
    }
    if (config.isDataAvailabilityTaskScheduler()) {
      dataAvailabilityTaskScheduler.shutdown();
    }
    if (modelDownloaderManager != null) {
      modelDownloaderManager.shutdown();
    }
  }
}
