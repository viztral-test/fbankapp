import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';

import { apiErrorMessage } from '../../core/api-error';
import { DEFAULT_CURRENCY } from '../../core/api.config';
import { TransactionPage, TransactionType } from '../../core/models';
import { WalletService } from '../../core/wallet.service';

const PAGE_SIZE = 10;

const TYPE_LABELS: Record<TransactionType, string> = {
  TOP_UP: 'Top-up',
  OPENING_BALANCE: 'Opening balance',
};

@Component({
  selector: 'app-transactions',
  imports: [RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './transactions.html',
  styleUrl: './transactions.css'
})
export class Transactions implements OnInit, OnDestroy {
  private readonly walletService = inject(WalletService);
  private request?: Subscription;

  protected readonly currency = computed(() => this.walletService.wallet()?.currency ?? DEFAULT_CURRENCY);
  protected readonly data = signal<TransactionPage | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load(0);
  }

  ngOnDestroy(): void {
    this.request?.unsubscribe();
  }

  protected load(page: number): void {
    // Drop any slower earlier request so its response cannot overwrite this one
    this.request?.unsubscribe();
    this.loading.set(true);
    this.error.set(null);

    this.request = this.walletService.loadTransactions(page, PAGE_SIZE).subscribe({
      next: (data) => {
        this.loading.set(false);
        this.data.set(data);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(apiErrorMessage(err, 'Could not load your transactions.'));
      },
    });
  }

  protected refresh(): void {
    this.load(this.data()?.page ?? 0);
  }

  protected typeLabel(type: TransactionType): string {
    return TYPE_LABELS[type] ?? type;
  }
}
