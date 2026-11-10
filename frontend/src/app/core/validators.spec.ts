import { FormControl } from '@angular/forms';

import { maxDecimals } from './validators';

// A validator is a plain function (control → errors | null), so no TestBed or HTTP is needed:
// put a value in a FormControl and call the validator on it.
describe('maxDecimals', () => {
  const twoPlaces = maxDecimals(2);
  const check = (value: unknown) => twoPlaces(new FormControl(value));

  it('accepts numbers within the limit', () => {
    expect(check(500)).toBeNull();
    expect(check(500.2)).toBeNull();
    expect(check(500.25)).toBeNull();
  });

  it('rejects numbers with too many decimal places', () => {
    expect(check(500.255)).toEqual({ maxDecimals: { places: 2 } });
  });

  it('leaves empty values to Validators.required', () => {
    expect(check(null)).toBeNull();
    expect(check('')).toBeNull();
  });

  it('is configurable: 4 places for units and NAVs', () => {
    const fourPlaces = maxDecimals(4);
    expect(fourPlaces(new FormControl(12.3456))).toBeNull();
    expect(fourPlaces(new FormControl(12.34567))).toEqual({ maxDecimals: { places: 4 } });
  });
});
