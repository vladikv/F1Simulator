import { ApplicationConfig } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { routes } from './app.routes';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';
import { leaderboardReducer } from './features/leaderboard/store/leaderboard.reducer';
import { LeaderboardEffects } from './features/leaderboard/store/leaderboard.effects';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    provideStore({ leaderboard: leaderboardReducer }),
    provideEffects([LeaderboardEffects])
  ]
};