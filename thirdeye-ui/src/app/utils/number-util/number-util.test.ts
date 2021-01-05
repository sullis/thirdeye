import numbro from "numbro";
import {
    formatLargeNumber,
    formatNumber,
    formatPercentage,
} from "./number-util";

jest.mock("numbro", () =>
    jest.fn().mockImplementation(
        (num: number): numbro.Numbro => {
            return ({
                format: mockFormat.mockReturnValue(num.toString()),
            } as unknown) as numbro.Numbro;
        }
    )
);

describe("Number Util", () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    afterAll(() => {
        jest.restoreAllMocks();
    });

    test("formatNumber should invoke numbro.format with appropriate default input and return result", () => {
        expect(formatNumber(1)).toEqual("1");
        expect(mockFormat).toHaveBeenCalledWith({
            thousandSeparated: true,
            mantissa: 2,
            optionalMantissa: true,
        });
    });

    test("formatNumber should invoke numbro.format with appropriate input and return result", () => {
        expect(formatNumber(1, 1, false)).toEqual("1");
        expect(mockFormat).toHaveBeenCalledWith({
            thousandSeparated: true,
            mantissa: 1,
            optionalMantissa: false,
        });
    });

    test("formatLargeNumber should invoke numbro.format with appropriate input and return result", () => {
        expect(formatLargeNumber(1)).toEqual("1");
        expect(mockFormat).toHaveBeenCalledWith({
            average: true,
            lowPrecision: false,
        });
    });

    test("formatPercentage should invoke numbro.format with appropriate default input and return result", () => {
        expect(formatPercentage(1)).toEqual("1");
        expect(mockFormat).toHaveBeenCalledWith({
            output: "percent",
            thousandSeparated: true,
            mantissa: 2,
            optionalMantissa: true,
        });
    });

    test("formatPercentage should invoke numbro.format with appropriate input and return result", () => {
        expect(formatPercentage(1, 1, false)).toEqual("1");
        expect(mockFormat).toHaveBeenCalledWith({
            output: "percent",
            thousandSeparated: true,
            mantissa: 1,
            optionalMantissa: false,
        });
    });
});

const mockFormat = jest.fn();
