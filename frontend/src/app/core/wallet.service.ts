import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { API_BASE_URL } from './api.config';
import { AddMoneyRequest, AddMoneyResponse, TransactionPage, Wallet } from './models';

@Injectable({ providedIn: 'root' })
export class WalletService {
  private readonly http = inject(HttpClient);
  private readonly walletState = signal<Wallet | null>(null);

  /** Latest balance returned by the API, shared by every wallet screen. */
  readonly wallet = this.walletState.asReadonly();

  loadBalance(): Observable<Wallet> {
    return this.http
      .get<Wallet>(`${API_BASE_URL}/api/wallet/balance`)
      .pipe(tap((wallet) => this.walletState.set(wallet)));
  }

  /** Not idempotent on the server: every call that reaches it adds money again. */
  addMoney(request: AddMoneyRequest): Observable<AddMoneyResponse> {
    return this.http
      .post<AddMoneyResponse>(`${API_BASE_URL}/api/wallet/add-money`, request)
      .pipe(tap((response) => this.walletState.set(response.wallet)));
  }

  loadTransactions(page: number, size: number): Observable<TransactionPage> {
    return this.http.get<TransactionPage>(`${API_BASE_URL}/api/wallet/transactions`, {
      params: { page, size },
    });
  }

  clear(): void {
    this.walletState.set(null);
  }
}
