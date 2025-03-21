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
import {User} from './user';
import {HttpClient} from '@angular/common/http';
import {mergeMap, Observable, ReplaySubject, Subject, take} from 'rxjs';
import {UserNavigateResponse} from "./request-response/user-navigate-response";

@Injectable({
  providedIn: 'root'
})
export class CurrentUserService {

  private readonly currentUserUrl: string;

  private currentUser$: Subject<User> = null;

  constructor(@Inject('BASE_URL') baseUrl: string,
              private http: HttpClient) {

    this.currentUserUrl = `${baseUrl}/api/users/current`;
  }

  getCurrentUser$(): Observable<User> {
    return this.currentUser$ || this.refreshCurrentUser$();
  }

  refreshCurrentUser$() {
    if (!this.currentUser$) {
      this.currentUser$ = new ReplaySubject(1);
    }
    return this.http.get<User>(this.currentUserUrl).pipe(
      mergeMap(currentUser => {
        this.currentUser$.next(currentUser);
        return this.currentUser$;
      }));
  }

  getCurrentUser(): Observable<User> {
    return this.getCurrentUser$().pipe(take(1));
  }

  refreshCurrentUser(): Observable<User> {
    return this.refreshCurrentUser$().pipe(take(1));
  }

  navigate(destinationServerId?: number): Observable<UserNavigateResponse> {
    return this.http.post(`${this.currentUserUrl}/navigate`, {destinationServerId: destinationServerId});
  }
}
