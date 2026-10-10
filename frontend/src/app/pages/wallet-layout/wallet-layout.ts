import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { apiErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { WalletService } from '../../core/wallet.service';

/** Frame around the signed-in screens: navigation, logout and the current balance. */
@Component({
  selector: 'app-wallet-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CurrencyPipe, DatePipe],
  templateUrl: './wallet-layout.html',
  styleUrl: './wallet-layout.css'
})
export class WalletLayout implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly walletService = inject(WalletService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly session = this.auth.session;
  protected readonly wallet = this.walletService.wallet;
  protected readonly balanceLoading = signal(false);
  protected readonly balanceError = signal<string | null>(null);
  protected readonly loggingOut = signal(false);

  ngOnInit(): void {
    // Never show a balance left over from a previous sign-in
    this.walletService.clear();
    this.loadBalance();
  }

  protected loadBalance(): void {
    this.balanceLoading.set(true);
    this.balanceError.set(null);
    this.walletService
      .loadBalance()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.balanceLoading.set(false),
        error: (err: unknown) => {
          this.balanceLoading.set(false);
          this.balanceError.set(apiErrorMessage(err, 'Could not load your balance.'));
        },
      });
  }

  protected logout(): void {
    if (this.loggingOut()) {
      return;
    }
    this.loggingOut.set(true);
    this.auth.logout().subscribe(() => {
      this.walletService.clear();
      void this.router.navigate(['/login']);
    });
  }
}
