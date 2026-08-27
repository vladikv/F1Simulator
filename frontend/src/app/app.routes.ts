import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

// Standalone components are referenced directly via loadComponent —
// each route lazy-loads its own JS chunk, so the initial bundle only
// contains what's needed for the first screen.
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/races/race-list.component').then(m => m.RaceListComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'strategy/:raceId/:driverId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/strategy-builder/strategy-builder.component').then(m => m.StrategyBuilderComponent)
  },
  {
    path: 'leaderboard',
    loadComponent: () => import('./features/leaderboard/leaderboard.component').then(m => m.LeaderboardComponent)
  }
];
