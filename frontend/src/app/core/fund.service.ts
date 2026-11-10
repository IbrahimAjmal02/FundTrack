import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Fund } from './models';

/** Talks to the backend's FundController. All URLs go through the dev proxy (/api → :8080). */
@Service()
export class FundService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/funds';

  /** GET /funds: every fund (for the Place order dropdown). */
  getFunds(): Observable<Fund[]> {
    return this.http.get<Fund[]>(this.baseUrl);
  }
}
