import { Grid } from "@material-ui/core";
import { cloneDeep, toNumber } from "lodash";
import { useSnackbar } from "notistack";
import React, { FunctionComponent, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useHistory, useParams } from "react-router-dom";
import { useAppBreadcrumbs } from "../../components/app-breadcrumbs/app-breadcrumbs.component";
import { useDialog } from "../../components/dialogs/dialog-provider/dialog-provider.component";
import { DialogType } from "../../components/dialogs/dialog-provider/dialog-provider.interfaces";
import { SubscriptionGroupCard } from "../../components/entity-cards/subscription-group-card/subscription-group-card.component";
import {
    SubscriptionGroupAlert,
    SubscriptionGroupCardData,
} from "../../components/entity-cards/subscription-group-card/subscription-group-card.interfaces";
import { LoadingIndicator } from "../../components/loading-indicator/loading-indicator.component";
import { NoDataIndicator } from "../../components/no-data-indicator/no-data-indicator.component";
import { PageContents } from "../../components/page-contents/page-contents.component";
import { SubscriptionGroupAlertsAccordian } from "../../components/subscription-group-alerts-accordian/subscription-group-alerts-accordian.component";
import { SubscriptionGroupEmailsAccordian } from "../../components/subscription-group-emails-accordian/subscription-group-emails-accordian.component";
import { getAllAlerts } from "../../rest/alerts/alerts.rest";
import { Alert } from "../../rest/dto/alert.interfaces";
import {
    EmailScheme,
    SubscriptionGroup,
} from "../../rest/dto/subscription-group.interfaces";
import {
    deleteSubscriptionGroup,
    getSubscriptionGroup,
    updateSubscriptionGroup,
} from "../../rest/subscription-groups/subscription-groups.rest";
import { isValidNumberId } from "../../utils/params/params.util";
import { getSubscriptionGroupsAllPath } from "../../utils/routes/routes.util";
import {
    getErrorSnackbarOption,
    getSuccessSnackbarOption,
} from "../../utils/snackbar/snackbar.util";
import { getSubscriptionGroupCardData } from "../../utils/subscription-groups/subscription-groups.util";
import { SubscriptionGroupsDetailPageParams } from "./subscription-groups-detail-page.interfaces";

export const SubscriptionGroupsDetailPage: FunctionComponent = () => {
    const [loading, setLoading] = useState(true);
    const [
        subscriptionGroupCardData,
        setSubscriptionGroupCardData,
    ] = useState<SubscriptionGroupCardData>();
    const [alerts, setAlerts] = useState<Alert[]>([]);
    const { setPageBreadcrumbs } = useAppBreadcrumbs();
    const { showDialog } = useDialog();
    const { enqueueSnackbar } = useSnackbar();
    const params = useParams<SubscriptionGroupsDetailPageParams>();
    const history = useHistory();
    const { t } = useTranslation();

    useEffect(() => {
        setPageBreadcrumbs([]);
        fetchSubscriptionGroup();
    }, []);

    const onDeleteSubscriptionGroup = (
        subscriptionGroupCardData: SubscriptionGroupCardData
    ): void => {
        if (!subscriptionGroupCardData) {
            return;
        }

        showDialog({
            type: DialogType.ALERT,
            text: t("message.delete-confirmation", {
                name: subscriptionGroupCardData.name,
            }),
            okButtonLabel: t("label.delete"),
            onOk: (): void => {
                onDeleteSubscriptionGroupConfirmation(
                    subscriptionGroupCardData
                );
            },
        });
    };

    const onDeleteSubscriptionGroupConfirmation = (
        subscriptionGroupCardData: SubscriptionGroupCardData
    ): void => {
        if (!subscriptionGroupCardData) {
            return;
        }

        deleteSubscriptionGroup(subscriptionGroupCardData.id)
            .then((): void => {
                enqueueSnackbar(
                    t("message.delete-success", {
                        entity: t("label.subscription-group"),
                    }),
                    getSuccessSnackbarOption()
                );

                // Redirect to subscription groups all path
                history.push(getSubscriptionGroupsAllPath());
            })
            .catch((): void => {
                enqueueSnackbar(
                    t("message.delete-error", {
                        entity: t("label.subscription-group"),
                    }),
                    getErrorSnackbarOption()
                );
            });
    };

    const onSubscriptionGroupAlertsChange = (
        subscriptionGroupAlerts: SubscriptionGroupAlert[]
    ): void => {
        if (
            !subscriptionGroupCardData ||
            !subscriptionGroupCardData.subscriptionGroup
        ) {
            return;
        }

        // Create a copy of subscription group and update alerts
        const subscriptionGroupCopy = cloneDeep(
            subscriptionGroupCardData.subscriptionGroup
        );
        subscriptionGroupCopy.alerts = subscriptionGroupAlerts as Alert[];
        saveUpdatedSubscriptionGroup(subscriptionGroupCopy);
    };

    const onSubscriptionGroupEmailsChange = (emails: string[]): void => {
        if (
            !subscriptionGroupCardData ||
            !subscriptionGroupCardData.subscriptionGroup
        ) {
            return;
        }

        // Create a copy of subscription group and update emails
        const subscriptionGroupCopy = cloneDeep(
            subscriptionGroupCardData.subscriptionGroup
        );
        if (
            subscriptionGroupCopy.notificationSchemes &&
            subscriptionGroupCopy.notificationSchemes.email
        ) {
            subscriptionGroupCopy.notificationSchemes.email.to = emails;
        } else {
            subscriptionGroupCopy.notificationSchemes = {
                email: {
                    to: emails,
                } as EmailScheme,
            };
        }
        saveUpdatedSubscriptionGroup(subscriptionGroupCopy);
    };

    const fetchSubscriptionGroup = (): void => {
        // Validate id from URL
        if (!isValidNumberId(params.id)) {
            enqueueSnackbar(
                t("message.invalid-id", {
                    entity: t("label.subscription-group"),
                    id: params.id,
                }),
                getErrorSnackbarOption()
            );
            setLoading(false);

            return;
        }

        Promise.allSettled([
            getSubscriptionGroup(toNumber(params.id)),
            getAllAlerts(),
        ])
            .then(([subscriptionGroupResponse, alertsResponse]): void => {
                // Determine if any of the calls failed
                if (
                    subscriptionGroupResponse.status === "rejected" ||
                    alertsResponse.status === "rejected"
                ) {
                    enqueueSnackbar(
                        t("message.fetch-error"),
                        getErrorSnackbarOption()
                    );
                }

                // Attempt to gather data
                let fetchedAlerts: Alert[] = [];
                if (alertsResponse.status === "fulfilled") {
                    fetchedAlerts = alertsResponse.value;
                    setAlerts(fetchedAlerts);
                }
                if (subscriptionGroupResponse.status === "fulfilled") {
                    setSubscriptionGroupCardData(
                        getSubscriptionGroupCardData(
                            subscriptionGroupResponse.value,
                            fetchedAlerts
                        )
                    );
                }
            })
            .finally((): void => {
                setLoading(false);
            });
    };

    const saveUpdatedSubscriptionGroup = (
        subscriptionGroup: SubscriptionGroup
    ): void => {
        if (!subscriptionGroup) {
            return;
        }

        updateSubscriptionGroup(subscriptionGroup)
            .then((subscriptionGroup: SubscriptionGroup): void => {
                enqueueSnackbar(
                    t("message.update-success", {
                        entity: t("label.subscription-group"),
                    }),
                    getSuccessSnackbarOption()
                );

                // Replace updated subscription group as fetched subscription group
                setSubscriptionGroupCardData(
                    getSubscriptionGroupCardData(subscriptionGroup, alerts)
                );
            })
            .catch((): void => {
                enqueueSnackbar(
                    t("message.update-error", {
                        entity: t("label.subscription-group"),
                    }),
                    getErrorSnackbarOption()
                );
            });
    };

    if (loading) {
        return <LoadingIndicator />;
    }

    return (
        <PageContents
            centered
            hideTimeRange
            title={
                subscriptionGroupCardData ? subscriptionGroupCardData.name : ""
            }
        >
            {subscriptionGroupCardData && (
                <Grid container>
                    {/* Subscription Group */}
                    <Grid item sm={12}>
                        <SubscriptionGroupCard
                            hideViewDetailsLinks
                            subscriptionGroupCardData={
                                subscriptionGroupCardData
                            }
                            onDelete={onDeleteSubscriptionGroup}
                        />
                    </Grid>

                    {/* Subscribed alerts */}
                    <Grid item sm={12}>
                        <SubscriptionGroupAlertsAccordian
                            alerts={alerts}
                            subscriptionGroupCardData={
                                subscriptionGroupCardData
                            }
                            title={t("label.subscribe-alerts")}
                            onChange={onSubscriptionGroupAlertsChange}
                        />
                    </Grid>

                    {/* Subscribed emails */}
                    <Grid item sm={12}>
                        <SubscriptionGroupEmailsAccordian
                            subscriptionGroupCardData={
                                subscriptionGroupCardData
                            }
                            title={t("label.subscribe-emails")}
                            onChange={onSubscriptionGroupEmailsChange}
                        />
                    </Grid>
                </Grid>
            )}

            {/* No data available message */}
            {!subscriptionGroupCardData && <NoDataIndicator />}
        </PageContents>
    );
};
