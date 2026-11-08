import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // withComponentInputBinding: route params like /clients/:id arrive as component inputs
    provideRouter(routes, withComponentInputBinding()),
    // Makes HttpClient injectable in your services (uses the browser's fetch under the hood)
    provideHttpClient(withFetch()),
  ],
};
