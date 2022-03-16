import { Card, CardContent, MenuItem, TextField } from "@material-ui/core";
import React, { FunctionComponent, useState } from "react";
import { useTranslation } from "react-i18next";
import {
    NotificationTypeV1,
    useNotificationProviderV1,
} from "../../platform/components";
import { updateAnomalyFeedback } from "../../rest/anomalies/anomalies.rest";
import { AnomalyFeedbackType } from "../../rest/dto/anomaly.interfaces";
import { useDialog } from "../dialogs/dialog-provider/dialog-provider.component";
import { DialogType } from "../dialogs/dialog-provider/dialog-provider.interfaces";
import { AnomalyFeedbackProps } from "./anomaly-feedback.interfaces";

const OPTION_TO_DESCRIPTIONS = {
    [AnomalyFeedbackType.ANOMALY.valueOf()]: "Anomaly - unexpected",
    [AnomalyFeedbackType.ANOMALY_EXPECTED.valueOf()]:
        "Anomaly - Expected temporary change",
    [AnomalyFeedbackType.ANOMALY_NEW_TREND.valueOf()]:
        "Anomaly - Permanent change",
    [AnomalyFeedbackType.NOT_ANOMALY.valueOf()]: "Not an anomaly",
    [AnomalyFeedbackType.NO_FEEDBACK.valueOf()]: "No feedback",
};

export const AnomalyFeedback: FunctionComponent<AnomalyFeedbackProps> = ({
    anomalyId,
    anomalyFeedback,
    className,
}) => {
    const [currentlySelected, setCurrentlySelected] =
        useState<AnomalyFeedbackType>(anomalyFeedback.type);
    const { showDialog } = useDialog();
    const { notify } = useNotificationProviderV1();
    const { t } = useTranslation();

    const handleChange = (
        event: React.ChangeEvent<{ value: unknown }>
    ): void => {
        const newSelectedFeedbackType = event.target
            .value as string as AnomalyFeedbackType;

        if (
            newSelectedFeedbackType &&
            newSelectedFeedbackType !== currentlySelected
        ) {
            showDialog({
                type: DialogType.ALERT,
                text: t("message.change-confirmation-to", {
                    value: `"${OPTION_TO_DESCRIPTIONS[newSelectedFeedbackType]}"`,
                }),
                okButtonLabel: t("label.change"),
                onOk: () => handleFeedbackChangeOk(newSelectedFeedbackType),
            });
        }
    };

    const handleFeedbackChangeOk = (
        feedbackType: AnomalyFeedbackType
    ): void => {
        const updateRequestPayload = {
            ...anomalyFeedback,
            type: feedbackType,
        };
        updateAnomalyFeedback(anomalyId, updateRequestPayload)
            .then(() => {
                notify(
                    NotificationTypeV1.Success,
                    t("message.update-success", {
                        entity: t("label.anomaly-feedback"),
                    })
                );
                setCurrentlySelected(feedbackType);
            })
            .catch(() => {
                notify(
                    NotificationTypeV1.Error,
                    t("message.update-error", {
                        entity: t("label.anomaly-feedback"),
                    })
                );
            });
    };

    return (
        <Card className={className} variant="outlined">
            <CardContent>
                <TextField
                    fullWidth
                    select
                    InputLabelProps={{
                        style: {
                            backgroundColor: "white",
                            paddingRight: "5px",
                        },
                    }}
                    id="anomaly-feedback-select"
                    label="Is this an anomaly?"
                    value={currentlySelected}
                    onChange={handleChange}
                >
                    {Object.keys(OPTION_TO_DESCRIPTIONS).map(
                        (optionKey: string) => (
                            <MenuItem key={optionKey} value={optionKey}>
                                {OPTION_TO_DESCRIPTIONS[optionKey]}
                            </MenuItem>
                        )
                    )}
                </TextField>
            </CardContent>
        </Card>
    );
};