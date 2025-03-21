/*
 * Copyright (c) 2022-2025 SpeculativeCoder (https://github.com/SpeculativeCoder)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import {Inject, Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Csrf} from "./csrf";
import {mergeMap, ReplaySubject, Subject} from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CsrfService {

  private readonly csrfUrl: string;

  private csrf$: Subject<Csrf> = null;

  constructor(@Inject('BASE_URL') baseUrl: string,
              private http: HttpClient) {

    this.csrfUrl = `${baseUrl}/api/csrf`;
  }

  getCsrf() {
    return this.csrf$ || this.refreshCsrf();
  }

  /**
   * Force refresh the CSRF token information.
   */
  refreshCsrf() {
    if (!this.csrf$) {
      this.csrf$ = new ReplaySubject(1);
    }
    return this.http.get<Csrf>(this.csrfUrl).pipe(
      mergeMap(csrf => {
        this.csrf$.next(csrf);
        return this.csrf$;
      }));
  }

  /**
   * Clear CSRF token information, forcing it to be obtained again when next needed.
   * Typically, this is called when the current user changes due to register/login etc.
   */
  clearCsrf() {
    this.csrf$ = null;
  }
}
