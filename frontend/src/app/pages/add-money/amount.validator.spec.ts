import { FormControl } from '@angular/forms';

import { amountValidator } from './amount.validator';

describe('amountValidator', () => {
  const validate = (value: string) => amountValidator(new FormControl(value));

  it('accepts amounts within the backend limits', () => {
    for (const value of ['0.01', '25.5', '25.50', '100', ' 10000 ', '10000.00']) {
      expect(validate(value)).toBeNull();
    }
  });

  it('requires a value', () => {
    expect(validate('')).toEqual({ required: true });
    expect(validate('   ')).toEqual({ required: true });
  });

  it('rejects anything that is not a plain number with at most 2 decimals', () => {
    for (const value of ['abc', '1.234', '-5', '1e3', '1,000', '.5', '5.']) {
      expect(validate(value)).toEqual({ format: true });
    }
  });

  it('enforces the minimum and maximum', () => {
    expect(validate('0')).toEqual({ min: true });
    expect(validate('0.00')).toEqual({ min: true });
    expect(validate('10000.01')).toEqual({ max: true });
  });
});
