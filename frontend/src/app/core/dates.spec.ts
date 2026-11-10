import { toIsoDate } from './dates';

describe('toIsoDate', () => {
  it('formats a date as YYYY-MM-DD', () => {
    expect(toIsoDate(new Date(2026, 10, 10))).toBe('2026-11-10'); // months are 0-based: 10 = November
  });

  it('zero-pads single-digit months and days', () => {
    expect(toIsoDate(new Date(2026, 0, 5))).toBe('2026-01-05');
  });

  it('uses the LOCAL date, even late in the evening (toISOString would roll over to UTC tomorrow)', () => {
    expect(toIsoDate(new Date(2026, 10, 10, 23, 30))).toBe('2026-11-10');
  });
});
