import { LinePath } from "@visx/visx";
import React, { FunctionComponent } from "react";
import { Dimension } from "../../../../utils/material-ui/dimension.util";
import { Palette } from "../../../../utils/material-ui/palette.util";
import { AlertEvaluationTimeSeriesCurrentPlotProps } from "./alert-evaluation-time-series-current-plot.interfaces";

export const AlertEvaluationTimeSeriesCurrentPlot: FunctionComponent<AlertEvaluationTimeSeriesCurrentPlotProps> = (
    props: AlertEvaluationTimeSeriesCurrentPlotProps
) => {
    return (
        <LinePath
            data={props.alertEvaluationTimeSeriesPoints}
            defined={(alertEvaluationTimeSeriesPoint) =>
                isFinite(alertEvaluationTimeSeriesPoint.current)
            }
            stroke={Palette.COLOR_VISUALIZATION_STROKE_CURRENT}
            strokeWidth={Dimension.WIDTH_VISUALIZATION_STROKE_CURRENT}
            x={(alertEvaluationTimeSeriesPoint) =>
                props.xScale(alertEvaluationTimeSeriesPoint.timestamp)
            }
            y={(alertEvaluationTimeSeriesPoint) =>
                props.yScale(alertEvaluationTimeSeriesPoint.current)
            }
        />
    );
};
