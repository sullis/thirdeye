/*
 * Copyright 2023 StarTree Inc
 *
 * Licensed under the StarTree Community License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.startree.ai/legal/startree-community-license
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT * WARRANTIES OF ANY KIND,
 * either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under
 * the License.
 */
import {
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    FormControlLabel,
    Grid,
    Radio,
    RadioGroup,
    TextField,
    Typography,
} from "@material-ui/core";
import { Autocomplete } from "@material-ui/lab";
import React, { FunctionComponent, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
    NotificationTypeV1,
    useNotificationProviderV1,
} from "../../../platform/components";
import { ActionStatus } from "../../../rest/actions.interfaces";
import { useUpdateAnomalyFeedback } from "../../../rest/anomalies/anomaly.actions";
import {
    AnomalyCause,
    AnomalyFeedback,
    AnomalyFeedbackType,
} from "../../../rest/dto/anomaly.interfaces";
import {
    ALL_OPTIONS_TO_DESCRIPTIONS,
    ANOMALY_OPTIONS_TO_DESCRIPTIONS,
} from "../../../utils/anomalies/anomalies.util";
import { notifyIfErrors } from "../../../utils/notifications/notifications.util";
import { AnomalyFeedbackReasonOption } from "../anomaly-feedback.interfaces";
import {
    AnomalyFeedbackModalProps,
    ANOMALY_FEEDBACK_TEST_IDS,
} from "./anomaly-feedback-modal.interfaces";
import { REASONS } from "./anomaly-feedback-modal.utils";

export const AnomalyFeedbackModal: FunctionComponent<AnomalyFeedbackModalProps> =
    ({ anomalyId, anomalyFeedback, trigger, showNo, onFeedbackUpdate }) => {
        const { t } = useTranslation();
        const [isOpen, setIsOpen] = useState(false);
        const { notify } = useNotificationProviderV1();

        const { updateAnomalyFeedback, status, errorMessages } =
            useUpdateAnomalyFeedback();

        const [selectedFeedbackOption, setSelectedFeedbackOption] =
            useState<AnomalyFeedbackType>(() => {
                if (anomalyFeedback) {
                    return anomalyFeedback.type;
                }

                return AnomalyFeedbackType.ANOMALY.valueOf() as AnomalyFeedbackType;
            });

        const [localComment, setLocalComment] = useState<string>(() => {
            if (anomalyFeedback) {
                try {
                    const parsed = JSON.parse(anomalyFeedback.comment);

                    return parsed.comment;
                } catch {
                    return anomalyFeedback.comment;
                }
            }

            return "";
        });

        const [reason, setReason] =
            useState<AnomalyFeedbackReasonOption | null>(() => {
                if (anomalyFeedback) {
                    if (anomalyFeedback.cause) {
                        return (
                            REASONS.find(
                                (candidate) =>
                                    candidate.serverValue ===
                                    anomalyFeedback.cause
                            ) || null
                        );
                    }
                    /**
                     * This uses a stop gap solution using comment field as json string storage
                     * It used to be an array
                     */
                    try {
                        const parsed = JSON.parse(anomalyFeedback.comment);

                        const reasonObjects: AnomalyFeedbackReasonOption[] = [];

                        parsed.reasons.forEach((reason: AnomalyCause) => {
                            const objectWithSameEnum = REASONS.find(
                                (candidate) => candidate.serverValue === reason
                            );

                            objectWithSameEnum &&
                                reasonObjects.push(objectWithSameEnum);
                        });

                        return reasonObjects[0];
                    } catch {
                        return null;
                    }
                }

                return null;
            });

        useEffect(() => {
            if (status === ActionStatus.Done) {
                notify(
                    NotificationTypeV1.Success,
                    t("message.update-success", {
                        entity: t("label.anomaly-feedback"),
                    })
                );

                setIsOpen(false);
            } else if (status === ActionStatus.Error) {
                notifyIfErrors(
                    status,
                    errorMessages,
                    notify,
                    t("message.update-error", {
                        entity: t("label.anomaly-feedback"),
                    })
                );
            }
        }, [status]);

        const handleSaveClick = (): void => {
            const feedbackObject: AnomalyFeedback = {
                ...anomalyFeedback,
                type: selectedFeedbackOption,
                comment: localComment,
            };

            if (reason) {
                feedbackObject.cause = reason.serverValue;
            }

            if (anomalyFeedback?.id) {
                feedbackObject.id = anomalyFeedback.id;
            }

            updateAnomalyFeedback(anomalyId, feedbackObject).then(() => {
                onFeedbackUpdate(feedbackObject);
            });
        };

        const descriptionListToUse = showNo
            ? ALL_OPTIONS_TO_DESCRIPTIONS
            : ANOMALY_OPTIONS_TO_DESCRIPTIONS;

        return (
            <>
                {trigger(() => setIsOpen(true))}

                <Dialog open={isOpen} onClose={() => setIsOpen(false)}>
                    <DialogTitle>{t("label.anomaly-confirmation")}</DialogTitle>
                    <DialogContent dividers>
                        <Grid container>
                            <Grid item lg={4} md={4} sm={12} xs={12}>
                                <Typography variant="body2">
                                    {t("message.why-is-this-an-anomaly")}
                                </Typography>
                            </Grid>
                            <Grid item lg={8} md={8} sm={12} xs={12}>
                                <RadioGroup
                                    value={selectedFeedbackOption}
                                    onChange={(_, selected) =>
                                        setSelectedFeedbackOption(
                                            selected as AnomalyFeedbackType
                                        )
                                    }
                                >
                                    {Object.keys(descriptionListToUse).map(
                                        (k) => {
                                            return (
                                                <FormControlLabel
                                                    control={<Radio />}
                                                    key={k}
                                                    label={
                                                        ALL_OPTIONS_TO_DESCRIPTIONS[
                                                            k
                                                        ]
                                                    }
                                                    value={k}
                                                />
                                            );
                                        }
                                    )}
                                </RadioGroup>
                            </Grid>
                        </Grid>
                        <Grid container>
                            <Grid item lg={4} md={4} sm={12} xs={12}>
                                <Typography variant="body2">
                                    {t("message.what-are-the-reasons")}
                                </Typography>
                                <Typography variant="caption">
                                    ({t("label.optional")})
                                </Typography>
                            </Grid>
                            <Grid item lg={8} md={8} sm={12} xs={12}>
                                <FormControl fullWidth variant="outlined">
                                    <Autocomplete
                                        fullWidth
                                        getOptionLabel={(option) =>
                                            option.label
                                        }
                                        options={REASONS}
                                        renderInput={(params) => (
                                            <TextField
                                                {...params}
                                                placeholder={t(
                                                    "message.click-to-select-reasons"
                                                )}
                                                variant="outlined"
                                            />
                                        )}
                                        value={reason}
                                        onChange={(_, selected) => {
                                            setReason(selected);
                                        }}
                                    />
                                </FormControl>
                            </Grid>
                        </Grid>
                        <Grid container>
                            <Grid item lg={4} md={4} sm={12} xs={12}>
                                <Typography variant="body2">
                                    {t("label.comments")}
                                </Typography>
                                <Typography variant="caption">
                                    ({t("label.optional")})
                                </Typography>
                            </Grid>
                            <Grid item lg={8} md={8} sm={12} xs={12}>
                                <TextField
                                    fullWidth
                                    multiline
                                    data-testid={
                                        ANOMALY_FEEDBACK_TEST_IDS.COMMENT_INPUT
                                    }
                                    defaultValue={localComment}
                                    minRows={3}
                                    onChange={(e) =>
                                        setLocalComment(e.target.value)
                                    }
                                />
                            </Grid>
                        </Grid>
                    </DialogContent>
                    <DialogActions>
                        <Button
                            color="primary"
                            disabled={status === ActionStatus.Working}
                            onClick={() => setIsOpen(false)}
                        >
                            {t("label.cancel")}
                        </Button>
                        <Button
                            color="primary"
                            data-testid={ANOMALY_FEEDBACK_TEST_IDS.SUBMIT_BTN}
                            disabled={status === ActionStatus.Working}
                            onClick={handleSaveClick}
                        >
                            {t("label.save")}
                        </Button>
                    </DialogActions>
                </Dialog>
            </>
        );
    };
