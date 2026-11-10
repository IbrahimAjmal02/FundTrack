import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Fails if the number has more than `places` digits after the decimal point.
 * Frontend twin of the backend's @Digits(fraction = ...): amount → 2, units/NAV → 4.
 *
 *   maxDecimals(2): 500.25 ✓   500.255 ✗ → { maxDecimals: { places: 2 } }
 */
export function maxDecimals(places: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (value === null || value === '') {
      return null; // empty is Validators.required's job, not ours
    }
    const decimals = String(value).split('.')[1] ?? '';
    return decimals.length > places ? { maxDecimals: { places } } : null;
  };
}
