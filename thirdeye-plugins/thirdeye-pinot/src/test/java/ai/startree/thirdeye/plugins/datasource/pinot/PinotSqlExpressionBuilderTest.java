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
package ai.startree.thirdeye.plugins.datasource.pinot;

import static ai.startree.thirdeye.plugins.datasource.pinot.PinotSqlExpressionBuilder.extractSimpleDateFormatPattern;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.startree.thirdeye.spi.datasource.macro.SqlExpressionBuilder;
import org.joda.time.Period;
import org.testng.annotations.Test;

public class PinotSqlExpressionBuilderTest implements SqlExpressionBuilder {

  @Test
  public void testExtractSimpleDateFormatPatternWithNoPrefix() {
    final String timeColumnFormat = "yyyyMMdd";
    final String output = extractSimpleDateFormatPattern(timeColumnFormat);
    final String expected = "yyyyMMdd";

    assertThat(output).isEqualTo(expected);
  }

  @Test
  public void testExtractSimpleDateFormatPatternWithSimpleDateFormatPrefix() {
    final String timeColumnFormat = "SIMPLE_DATE_FORMAT:yyyyMMdd";
    final String output = extractSimpleDateFormatPattern(timeColumnFormat);
    final String expected = "yyyyMMdd";

    assertThat(output).isEqualTo(expected);
  }

  @Test
  public void testExtractSimpleDateFormatPatternWithFullPrefix() {
    final String timeColumnFormat = "1:DAYS:SIMPLE_DATE_FORMAT:yyyyMMdd";
    final String output = extractSimpleDateFormatPattern(timeColumnFormat);
    final String expected = "yyyyMMdd";

    assertThat(output).isEqualTo(expected);
  }

  @Test
  public void testExtractSimpleDateFormatPatternWithNewDateFormat() {
    final String timeColumnFormat = "SIMPLE_DATE_FORMAT|yyyyMMdd";
    final String output = extractSimpleDateFormatPattern(timeColumnFormat);
    final String expected = "yyyyMMdd";

    assertThat(output).isEqualTo(expected);
  }

  @Test
  public void testExtractSimpleDateFormatPatternWithNewDateFormatWithUnusedTimezone() {
    final String timeColumnFormat = "SIMPLE_DATE_FORMAT|yyyyMMdd|Europe/Paris";
    final String output = extractSimpleDateFormatPattern(timeColumnFormat);
    final String expected = "yyyyMMdd";

    assertThat(output).isEqualTo(expected);
  }

  @Test
  public void testExtractSimpleDateFormatPatternWithInvalidNewFormat() {
    final String timeColumnFormat = "SIMPLE_DATE_FORMAT|";
    assertThatThrownBy(() -> extractSimpleDateFormatPattern(timeColumnFormat)).isInstanceOf(
        IllegalArgumentException.class);
  }

  @Test
  public void testGetTimeGroupExpressionWithEscapedLiteralQuote() {
    final SqlExpressionBuilder pinotExpressionBuilder = new PinotSqlExpressionBuilder();
    final String output = pinotExpressionBuilder.getTimeGroupExpression("timeCol",
        "yyyyMMdd'T'HH:mm:ss'Z'",
        Period.days(1),
        null);
    final String expected = " DATETIMECONVERT(timeCol, '1:DAYS:SIMPLE_DATE_FORMAT:yyyyMMdd''T''HH:mm:ss''Z''', '1:MILLISECONDS:EPOCH', '1:DAYS') ";

    assertThat(output).isEqualTo(expected);
  }
}
