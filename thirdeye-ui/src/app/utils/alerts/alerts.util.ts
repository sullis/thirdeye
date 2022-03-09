import i18n from "i18next";
import { cloneDeep, isEmpty, omit } from "lodash";
import {
    Alert,
    AlertAnomalyDetectorNode,
    AlertEvaluation,
    AlertNodeType,
    EditableAlert,
} from "../../rest/dto/alert.interfaces";
import { SubscriptionGroup } from "../../rest/dto/subscription-group.interfaces";
import {
    UiAlert,
    UiAlertDatasetAndMetric,
    UiAlertSubscriptionGroup,
} from "../../rest/dto/ui-alert.interfaces";
import { deepSearchStringProperty } from "../search/search.util";

export const createDefaultAlert = (): EditableAlert => {
    return {
        name: "simple-threshold-template",
        description:
            "Sample threshold alert. Runs every hour. Change the template properties to run on your data",
        cron: "0 0 0 1/1 * ? *",
        template: {
            nodes: [
                {
                    name: "root",
                    type: "AnomalyDetector",
                    params: {
                        type: "THRESHOLD",
                        "component.timezone": "UTC",
                        "component.monitoringGranularity":
                            "${monitoringGranularity}",
                        "component.timestamp": "ts",
                        "component.metric": "met",
                        "component.max": "${max}",
                        "component.min": "${min}",
                        "anomaly.metric": "${aggregateFunction}(${metric})",
                    },
                    inputs: [
                        {
                            targetProperty: "current",
                            sourcePlanNode: "currentDataFetcher",
                            sourceProperty: "currentData",
                        },
                    ],
                },
                {
                    name: "currentDataFetcher",
                    type: "DataFetcher",
                    params: {
                        "component.dataSource": "${dataSource}",
                        "component.query":
                            "SELECT __timeGroup(\"${timeColumn}\", '${timeColumnFormat}'," +
                            " '${monitoringGranularity}') as ts, ${aggregateFunction}(${metric}) as met FROM " +
                            "${dataset} WHERE __timeFilter(ts) GROUP BY ts ORDER BY ts LIMIT 1000",
                    },
                    outputs: [
                        {
                            outputKey: "pinot",
                            outputName: "currentData",
                        },
                    ],
                },
            ],
            metadata: {
                datasource: {
                    name: "${dataSource}",
                },
                dataset: {
                    name: "${dataset}",
                },
                metric: {
                    name: "${metric}",
                },
            },
        },
        templateProperties: {
            dataSource: "pinotQuickStartAzure",
            dataset: "pageviews",
            aggregateFunction: "sum",
            metric: "views",
            monitoringGranularity: "P1D",
            timeColumn: "date",
            timeColumnFormat: "yyyyMMdd",
            max: "850000",
            min: "250000",
        },
    };
};

export const createEmptyUiAlert = (): UiAlert => {
    const noDataMarker = i18n.t("label.no-data-marker");

    return {
        id: -1,
        name: noDataMarker,
        active: false,
        activeText: noDataMarker,
        userId: -1,
        createdBy: noDataMarker,
        detectionTypes: [],
        datasetAndMetrics: [],
        subscriptionGroups: [],
        alert: null,
    };
};

export const createEmptyUiAlertDatasetAndMetric =
    (): UiAlertDatasetAndMetric => {
        const noDataMarker = i18n.t("label.no-data-marker");

        return {
            datasetId: -1,
            datasetName: noDataMarker,
            metricId: -1,
            metricName: noDataMarker,
        };
    };

export const createEmptyUiAlertSubscriptionGroup =
    (): UiAlertSubscriptionGroup => {
        return {
            id: -1,
            name: i18n.t("label.no-data-marker"),
        };
    };

export const createAlertEvaluation = (
    alert: Alert | EditableAlert,
    startTime: number,
    endTime: number
): AlertEvaluation => {
    return {
        alert: alert,
        start: startTime,
        end: endTime,
    } as AlertEvaluation;
};

export const getUiAlert = (
    alert: EditableAlert,
    subscriptionGroups: SubscriptionGroup[]
): UiAlert => {
    if (!alert) {
        return createEmptyUiAlert();
    }

    // Map subscription groups to alert ids
    const subscriptionGroupsToAlertIdsMap =
        mapSubscriptionGroupsToAlertIds(subscriptionGroups);

    return getUiAlertInternal(alert as Alert, subscriptionGroupsToAlertIdsMap);
};

export const getUiAlerts = (
    alerts: Alert[],
    subscriptionGroups: SubscriptionGroup[]
): UiAlert[] => {
    if (isEmpty(alerts)) {
        return [];
    }

    // Map subscription groups to alert ids
    const subscriptionGroupsToAlertIdsMap =
        mapSubscriptionGroupsToAlertIds(subscriptionGroups);

    const uiAlerts = [];
    for (const alert of alerts) {
        uiAlerts.push(
            getUiAlertInternal(alert, subscriptionGroupsToAlertIdsMap)
        );
    }

    return uiAlerts;
};

export const filterAlerts = (
    uiAlerts: UiAlert[],
    searchWords: string[]
): UiAlert[] => {
    if (isEmpty(uiAlerts)) {
        return [];
    }

    if (isEmpty(searchWords)) {
        return uiAlerts;
    }

    const filteredUiAlerts = [];
    for (const uiAlert of uiAlerts) {
        // Only the UI alert to be searched and not contained alert
        const uiAlertCopy = cloneDeep(uiAlert);
        uiAlertCopy.alert = null;

        for (const searchWord of searchWords) {
            if (
                deepSearchStringProperty(
                    uiAlertCopy,
                    // Check if string property value contains current search word
                    (value) =>
                        Boolean(value) &&
                        value.toLowerCase().indexOf(searchWord.toLowerCase()) >
                            -1
                )
            ) {
                filteredUiAlerts.push(uiAlert);

                break;
            }
        }
    }

    return filteredUiAlerts;
};

export const omitNonUpdatableData = (
    alert: Alert | EditableAlert
): EditableAlert => {
    const newAlert = omit(alert, "id");

    return newAlert as Alert;
};

const getUiAlertInternal = (
    alert: Alert,
    subscriptionGroupsToAlertIdsMap: Map<number, UiAlertSubscriptionGroup[]>
): UiAlert => {
    const uiAlert = createEmptyUiAlert();
    const noDataMarker = i18n.t("label.no-data-marker");

    // Maintain a copy of alert
    uiAlert.alert = alert;

    // Basic properties
    uiAlert.id = alert.id;
    uiAlert.name = alert.name || noDataMarker;
    uiAlert.active = Boolean(alert.active);
    uiAlert.activeText = alert.active
        ? i18n.t("label.active")
        : i18n.t("label.inactive");

    // User properties
    if (alert.owner) {
        uiAlert.userId = alert.owner.id;
        uiAlert.createdBy = alert.owner.principal || noDataMarker;
    }

    // Subscription groups
    if (subscriptionGroupsToAlertIdsMap) {
        uiAlert.subscriptionGroups =
            subscriptionGroupsToAlertIdsMap.get(alert.id) || [];
    }

    if (alert.template && alert.template.nodes) {
        alert.template.nodes.forEach((alertNode) => {
            if (alertNode.type === AlertNodeType.ANOMALY_DETECTOR.toString()) {
                if ((alertNode as AlertAnomalyDetectorNode).params) {
                    uiAlert.detectionTypes.push(
                        (alertNode as AlertAnomalyDetectorNode).params.type
                    );
                }
            }
        });
    }

    return uiAlert;
};

const mapSubscriptionGroupsToAlertIds = (
    subscriptionGroups: SubscriptionGroup[]
): Map<number, UiAlertSubscriptionGroup[]> => {
    const subscriptionGroupsToAlertIdsMap = new Map();

    if (isEmpty(subscriptionGroups)) {
        return subscriptionGroupsToAlertIdsMap;
    }

    for (const subscriptionGroup of subscriptionGroups) {
        if (isEmpty(subscriptionGroup.alerts)) {
            continue;
        }

        const uiAlertSubscriptionGroup = createEmptyUiAlertSubscriptionGroup();
        uiAlertSubscriptionGroup.id = subscriptionGroup.id;
        uiAlertSubscriptionGroup.name =
            subscriptionGroup.name || i18n.t("label.no-data-marker");

        for (const alert of subscriptionGroup.alerts) {
            const subscriptionGroups = subscriptionGroupsToAlertIdsMap.get(
                alert.id
            );
            if (subscriptionGroups) {
                // Add to existing list
                subscriptionGroups.push(uiAlertSubscriptionGroup);
            } else {
                // Create and add to list
                subscriptionGroupsToAlertIdsMap.set(alert.id, [
                    uiAlertSubscriptionGroup,
                ]);
            }
        }
    }

    return subscriptionGroupsToAlertIdsMap;
};
