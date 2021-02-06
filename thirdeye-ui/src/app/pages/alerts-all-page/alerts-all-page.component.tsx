import { Grid } from "@material-ui/core";
import { isEmpty } from "lodash";
import { useSnackbar } from "notistack";
import React, { FunctionComponent, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useAppBreadcrumbs } from "../../components/app-breadcrumbs/app-breadcrumbs.component";
import { useDialog } from "../../components/dialogs/dialog-provider/dialog-provider.component";
import { DialogType } from "../../components/dialogs/dialog-provider/dialog-provider.interfaces";
import { AlertCard } from "../../components/entity-cards/alert-card/alert-card.component";
import { AlertCardData } from "../../components/entity-cards/alert-card/alert-card.interfaces";
import { LoadingIndicator } from "../../components/loading-indicator/loading-indicator.component";
import { NoDataIndicator } from "../../components/no-data-indicator/no-data-indicator.component";
import { PageContents } from "../../components/page-contents/page-contents.component";
import { SearchBar } from "../../components/search-bar/search-bar.component";
import {
    deleteAlert,
    getAllAlerts,
    updateAlert,
} from "../../rest/alerts/alerts.rest";
import { Alert } from "../../rest/dto/alert.interfaces";
import { SubscriptionGroup } from "../../rest/dto/subscription-group.interfaces";
import { getAllSubscriptionGroups } from "../../rest/subscription-groups/subscription-groups.rest";
import {
    filterAlerts,
    getAlertCardData,
    getAlertCardDatas,
} from "../../utils/alerts/alerts.util";
import {
    getErrorSnackbarOption,
    getSuccessSnackbarOption,
} from "../../utils/snackbar/snackbar.util";

export const AlertsAllPage: FunctionComponent = () => {
    const [loading, setLoading] = useState(true);
    const [alertCardDatas, setAlertCardDatas] = useState<AlertCardData[]>([]);
    const [filteredAlertCardDatas, setFilteredAlertCardDatas] = useState<
        AlertCardData[]
    >([]);
    const [subscriptionGroups, setSubscriptionGroups] = useState<
        SubscriptionGroup[]
    >([]);
    const [searchWords, setSearchWords] = useState<string[]>([]);
    const { setPageBreadcrumbs } = useAppBreadcrumbs();
    const { showDialog } = useDialog();
    const { enqueueSnackbar } = useSnackbar();
    const { t } = useTranslation();

    useEffect(() => {
        setPageBreadcrumbs([]);
        fetchAllAlerts();
    }, []);

    useEffect(() => {
        // Fetched alerts or search changed, reset
        setFilteredAlertCardDatas(filterAlerts(alertCardDatas, searchWords));
    }, [alertCardDatas, searchWords]);

    const onAlertChange = (alertCardData: AlertCardData): void => {
        if (!alertCardData || !alertCardData.alert) {
            return;
        }

        updateAlert(alertCardData.alert)
            .then((alert: Alert): void => {
                enqueueSnackbar(
                    t("message.update-success", {
                        entity: t("label.alert"),
                    }),
                    getSuccessSnackbarOption()
                );

                // Replace updated alert in fetched alerts
                replaceAlertCardData(alert);
            })
            .catch((): void => {
                enqueueSnackbar(
                    t("message.update-error", {
                        entity: t("label.alert"),
                    }),
                    getErrorSnackbarOption()
                );
            });
    };

    const onDeleteAlert = (alertCardData: AlertCardData): void => {
        if (!alertCardData) {
            return;
        }

        showDialog({
            type: DialogType.ALERT,
            text: t("message.delete-confirmation", {
                name: alertCardData.name,
            }),
            okButtonLabel: t("label.delete"),
            onOk: (): void => {
                onDeleteAlertConfirmation(alertCardData);
            },
        });
    };

    const onDeleteAlertConfirmation = (alertCardData: AlertCardData): void => {
        if (!alertCardData) {
            return;
        }

        deleteAlert(alertCardData.id)
            .then((alert: Alert): void => {
                enqueueSnackbar(
                    t("message.delete-success", {
                        entity: t("label.alert"),
                    }),
                    getSuccessSnackbarOption()
                );

                // Remove deleted alert from fetched alerts
                removeAlertCardData(alert);
            })
            .catch((): void => {
                enqueueSnackbar(
                    t("message.delete-error", {
                        entity: t("label.alert"),
                    }),
                    getErrorSnackbarOption()
                );
            });
    };

    const fetchAllAlerts = (): void => {
        Promise.allSettled([getAllAlerts(), getAllSubscriptionGroups()])
            .then(([alertsResponse, subscriptionGroupsResponse]): void => {
                // Determine if any of the calls failed
                if (
                    alertsResponse.status === "rejected" ||
                    subscriptionGroupsResponse.status === "rejected"
                ) {
                    enqueueSnackbar(
                        t("message.fetch-error"),
                        getErrorSnackbarOption()
                    );
                }

                // Attempt to gather data
                let fetchedSubscriptionGroups: SubscriptionGroup[] = [];
                if (subscriptionGroupsResponse.status === "fulfilled") {
                    fetchedSubscriptionGroups =
                        subscriptionGroupsResponse.value;
                    setSubscriptionGroups(fetchedSubscriptionGroups);
                }
                if (alertsResponse.status === "fulfilled") {
                    setAlertCardDatas(
                        getAlertCardDatas(
                            alertsResponse.value,
                            fetchedSubscriptionGroups
                        )
                    );
                }
            })
            .finally((): void => {
                setLoading(false);
            });
    };

    const replaceAlertCardData = (alert: Alert): void => {
        if (!alert) {
            return;
        }

        setAlertCardDatas((alertCardDatas) =>
            alertCardDatas.map(
                (alertCardData: AlertCardData): AlertCardData => {
                    if (alertCardData.id === alert.id) {
                        // Replace
                        return getAlertCardData(alert, subscriptionGroups);
                    }

                    return alertCardData;
                }
            )
        );
    };

    const removeAlertCardData = (alert: Alert): void => {
        if (!alert) {
            return;
        }

        setAlertCardDatas((alertCardDatas) =>
            alertCardDatas.filter((alertCardData: AlertCardData): boolean => {
                return alertCardData.id !== alert.id;
            })
        );
    };

    if (loading) {
        return <LoadingIndicator />;
    }

    return (
        <PageContents centered hideAppBreadcrumbs title={t("label.alerts")}>
            <Grid container>
                {/* Search */}
                <Grid item sm={12}>
                    <SearchBar
                        autoFocus
                        setSearchQueryString
                        searchLabel={t("label.search-alerts")}
                        searchStatusLabel={t("label.search-count", {
                            count: filteredAlertCardDatas
                                ? filteredAlertCardDatas.length
                                : 0,
                            total: alertCardDatas ? alertCardDatas.length : 0,
                        })}
                        onChange={setSearchWords}
                    />
                </Grid>

                {/* Alerts */}
                {filteredAlertCardDatas &&
                    filteredAlertCardDatas.map(
                        (filteredAlertCardData, index) => (
                            <Grid item key={index} sm={12}>
                                <AlertCard
                                    alertCardData={filteredAlertCardData}
                                    searchWords={searchWords}
                                    onChange={onAlertChange}
                                    onDelete={onDeleteAlert}
                                />
                            </Grid>
                        )
                    )}
            </Grid>

            {/* No data available message */}
            {isEmpty(filteredAlertCardDatas) && isEmpty(searchWords) && (
                <NoDataIndicator />
            )}

            {/* No search results available message */}
            {isEmpty(filteredAlertCardDatas) && !isEmpty(searchWords) && (
                <NoDataIndicator text={t("message.no-search-results")} />
            )}
        </PageContents>
    );
};
