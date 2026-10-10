import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './core/auth.guard';
import { AddMoney } from './pages/add-money/add-money';
import { Login } from './pages/login/login';
import { Transactions } from './pages/transactions/transactions';
import { WalletLayout } from './pages/wallet-layout/wallet-layout';

export const routes: Routes = [
  { path: 'login', component: Login, canActivate: [guestGuard], title: 'Sign in · fbankapp' },
  {
    path: '',
    component: WalletLayout,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'transactions' },
      { path: 'transactions', component: Transactions, title: 'Transactions · fbankapp' },
      { path: 'add-money', component: AddMoney, title: 'Add money · fbankapp' },
    ],
  },
  { path: '**', redirectTo: '' },
];
