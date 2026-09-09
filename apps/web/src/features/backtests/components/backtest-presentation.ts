const finiteNumber = (value: string) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

const formatDecimal = (value: string, options: Intl.NumberFormatOptions, positiveSign = false) => {
  const parsed = finiteNumber(value);
  if (parsed === null) return value;
  const formatted = new Intl.NumberFormat("en-US", options).format(parsed);
  return positiveSign && parsed > 0 ? `+${formatted}` : formatted;
};

export const formatBacktestPercent = (value: string, positiveSign = false) => {
  const parsed = finiteNumber(value);
  if (parsed === null) return value;
  const percentage = parsed * 100;
  const formatted = new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(percentage);
  return `${positiveSign && percentage > 0 ? "+" : ""}${formatted}%`;
};

export const formatMoney = (value: string, currency?: string, positiveSign = false) => {
  const formatted = formatDecimal(
    value,
    { minimumFractionDigits: 2, maximumFractionDigits: 2 },
    positiveSign
  );
  return currency ? `${formatted} ${currency}` : formatted;
};

export const formatPrice = (value: string, currency?: string) => {
  const formatted = formatDecimal(value, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 8
  });
  return currency ? `${formatted} ${currency}` : formatted;
};

export const formatQuantity = (value: string) =>
  formatDecimal(value, { minimumFractionDigits: 0, maximumFractionDigits: 6 });

export const formatUtcDateTime = (value: string) => {
  const date = new Date(value);
  if (Number.isNaN(date.valueOf())) return value;
  return `${new Intl.DateTimeFormat("en-US", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
    timeZone: "UTC"
  }).format(date)} UTC`;
};

export const formatCount = (value: number) => new Intl.NumberFormat("en-US").format(value);

export const quoteCurrency = (tradingPair?: string | null) => {
  if (!tradingPair) return undefined;
  const separator = tradingPair.lastIndexOf("/");
  if (separator < 0 || separator === tradingPair.length - 1) return undefined;
  const quote = tradingPair.slice(separator + 1).trim();
  return quote || undefined;
};

export const valueTone = (value: string) => {
  const parsed = finiteNumber(value);
  if (parsed === null || parsed === 0) return "";
  return parsed > 0 ? "metric-positive" : "metric-negative";
};

export const humanizeBacktestValue = (value: string) => {
  const labels: Readonly<Record<string, string>> = {
    LONG: "Long",
    LONG_ONLY: "Long only",
    NEXT_CANDLE_OPEN: "Next candle open",
    STRATEGY_SELL: "Strategy sell signal",
    FORCED_FINAL_CLOSE: "Closed at dataset end"
  };
  return (
    labels[value] ??
    value
      .toLowerCase()
      .replaceAll("_", " ")
      .replace(/\b\w/g, (letter) => letter.toUpperCase())
  );
};

export const subtractDecimals = (left: string, right: string) => {
  const parsedLeft = parseDecimal(left);
  const parsedRight = parseDecimal(right);
  if (!parsedLeft || !parsedRight) return left;
  const scale = Math.max(parsedLeft.scale, parsedRight.scale);
  const leftValue = parsedLeft.value * 10n ** BigInt(scale - parsedLeft.scale);
  const rightValue = parsedRight.value * 10n ** BigInt(scale - parsedRight.scale);
  return decimalText(leftValue - rightValue, scale);
};

const parseDecimal = (value: string) => {
  const match = /^([+-]?)(\d+)(?:\.(\d+))?$/.exec(value.trim());
  if (!match) return null;
  const fraction = match[3] ?? "";
  const sign = match[1] === "-" ? -1n : 1n;
  return {
    value: sign * BigInt(`${match[2]}${fraction}`),
    scale: fraction.length
  };
};

const decimalText = (value: bigint, scale: number) => {
  const negative = value < 0n;
  const digits = (negative ? -value : value).toString().padStart(scale + 1, "0");
  if (scale === 0) return `${negative ? "-" : ""}${digits}`;
  const integer = digits.slice(0, -scale);
  const fraction = digits.slice(-scale).replace(/0+$/, "");
  return `${negative ? "-" : ""}${integer}${fraction ? `.${fraction}` : ""}`;
};
