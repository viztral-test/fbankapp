import { AbstractControl, ValidationErrors } from '@angular/forms';

// Same rules as the backend (backend/API.md): 0.01 to 10000.00, at most 2 decimal places
export const MIN_AMOUNT = 0.01;
export const MAX_AMOUNT = 10000;

const AMOUNT_PATTERN = /^\d+(\.\d{1,2})?$/;

/** Validates an amount typed as text; reports exactly one of `required`, `format`, `min` or `max`. */
export function amountValidator(control: AbstractControl): ValidationErrors | null {
  const raw = String(control.value ?? '').trim();
  if (!raw) {
    return { required: true };
  }
  if (!AMOUNT_PATTERN.test(raw)) {
    return { format: true };
  }
  const amount = Number(raw);
  if (amount < MIN_AMOUNT) {
    return { min: true };
  }
  if (amount > MAX_AMOUNT) {
    return { max: true };
  }
  return null;
}
