/**
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
import { Grid } from "@material-ui/core";
import { AxiosError } from "axios";
import { isEmpty } from "lodash";
import React, { FunctionComponent } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { AlertTemplateWizard } from "../../components/alert-template-wizard/altert-template-wizard.component";
import {
    HelpLinkIconV1,
    NotificationTypeV1,
    PageContentsGridV1,
    PageHeaderTextV1,
    PageHeaderV1,
    PageV1,
    TooltipV1,
    useNotificationProviderV1,
} from "../../platform/components";
import { createAlertTemplate } from "../../rest/alert-templates/alert-templates.rest";
import {
    AlertTemplate,
    NewAlertTemplate,
} from "../../rest/dto/alert-template.interfaces";
import { createDefaultAlertTemplate } from "../../utils/alert-templates/alert-templates.util";
import { THIRDEYE_DOC_LINK } from "../../utils/constants/constants.util";
import { getErrorMessages } from "../../utils/rest/rest.util";
import { getAlertTemplatesViewPath } from "../../utils/routes/routes.util";

export const AlertTemplatesCreatePage: FunctionComponent = () => {
    const navigate = useNavigate();
    const { t } = useTranslation();
    const { notify } = useNotificationProviderV1();

    const onAlertTemplateWizardFinish = (
        alertTemplate: NewAlertTemplate
    ): void => {
        if (!alertTemplate) {
            return;
        }

        createAlertTemplate(alertTemplate)
            .then((alertTemplate: AlertTemplate): void => {
                notify(
                    NotificationTypeV1.Success,
                    t("message.create-success", {
                        entity: t("label.alert-template"),
                    })
                );
                // Redirect to alert template view path
                alertTemplate.id &&
                    navigate(getAlertTemplatesViewPath(alertTemplate.id));
            })
            .catch((error: AxiosError): void => {
                const errMessages = getErrorMessages(error);

                isEmpty(errMessages)
                    ? notify(
                          NotificationTypeV1.Error,

                          t("message.create-error", {
                              entity: t("label.alert-template"),
                          })
                      )
                    : errMessages.map((err) =>
                          notify(NotificationTypeV1.Error, err)
                      );
            });
    };

    return (
        <PageV1>
            <PageHeaderV1>
                <PageHeaderTextV1>
                    {t("label.create-entity", {
                        entity: t("label.alert-template"),
                    })}
                    <TooltipV1
                        placement="top"
                        title={t("label.view-configuration-docs") as string}
                    >
                        <span>
                            <HelpLinkIconV1
                                displayInline
                                enablePadding
                                externalLink
                                href={`${THIRDEYE_DOC_LINK}/concepts/alert-configuration`}
                            />
                        </span>
                    </TooltipV1>
                </PageHeaderTextV1>
            </PageHeaderV1>
            <PageContentsGridV1>
                <Grid item xs={12}>
                    <AlertTemplateWizard<NewAlertTemplate>
                        alertTemplate={createDefaultAlertTemplate()}
                        onFinish={onAlertTemplateWizardFinish}
                    />
                </Grid>
            </PageContentsGridV1>
        </PageV1>
    );
};
