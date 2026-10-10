import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { apiErrorMessage, apiFieldErrors, isNetworkError } from '../../core/api-error';
import { DEFAULT_CURRENCY } from '../../core/api.config';
import { AddMoneyRequest, AddMoneyResponse } from '../../core/models';
import { WalletService } from '../../core/wallet.service';
import { MAX_AMOUNT, MIN_AMOUNT, amountValidator } from './amount.validator';

const NOTE_MAX_LENGTH = 80;

@Component({
  selector: 'app-add-money',
  imports: [ReactiveFormsModule, RouterLink, CurrencyPipe],
  templateUrl: './add-money.html',
  styleUrl: './add-money.css'
})
export class AddMoney {
  private readonly walletService = inject(WalletService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly minAmount = MIN_AMOUNT;
  protected readonly maxAmount = MAX_AMOUNT;
  protected readonly noteMaxLength = NOTE_MAX_LENGTH;

  protected readonly form = inject(FormBuilder).nonNullable.group({
    amount: ['', amountValidator],
    note: ['', Validators.maxLength(NOTE_MAX_LENGTH)],
  });

  protected readonly currency = computed(() => this.walletService.wallet()?.currency ?? DEFAULT_CURRENCY);
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly result = signal<AddMoneyResponse | null>(null);

  protected showError(name: 'amount' | 'note'): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  protected submit(): void {
    // The server adds money on every call it receives, so never send a second one while one is in flight
    if (this.submitting()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { amount, note } = this.form.getRawValue();
    const request: AddMoneyRequest = { amount: Number(amount.trim()) };
    if (note.trim()) {
      request.note = note.trim();
    }

    this.submitting.set(true);
    this.error.set(null);
    this.result.set(null);

    this.walletService
      .addMoney(request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.submitting.set(false);
          this.result.set(response);
          this.form.reset();
        },
        error: (err: unknown) => {
          this.submitting.set(false);
          this.handleError(err);
        },
      });
  }

  private handleError(err: unknown): void {
    const fieldErrors = apiFieldErrors(err);
    let appliedToField = false;
    for (const name of ['amount', 'note'] as const) {
      if (fieldErrors[name]) {
        this.form.controls[name].setErrors({ server: fieldErrors[name] });
        this.form.controls[name].markAsTouched();
        appliedToField = true;
      }
    }
    if (appliedToField) {
      return;
    }

    if (isNetworkError(err)) {
      // No response does not prove the top-up failed, and repeating it would add the money twice
      this.error.set(
        'No response from the server, so the top-up may or may not have gone through. ' +
          'Check your transactions before trying again.',
      );
      return;
    }
    this.error.set(apiErrorMessage(err, 'Could not add money. Please try again.'));
  }
}
