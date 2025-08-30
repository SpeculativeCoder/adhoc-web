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
import {mergeMap} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {CurrentUserService} from '../current-user.service';
import {CsrfService} from '../../system/csrf.service';

@Injectable({
  providedIn: 'root'
})
export class LoginService {

  private readonly loginUrl: string;

  constructor(@Inject('BASE_URL') baseUrl: string,
              private http: HttpClient,
              private currentUserService: CurrentUserService,
              private csrfService: CsrfService) {

    this.loginUrl = `${baseUrl}/adhoc_api/login`;
  }

  login(usernameOrEmail: string, password: string, rememberMe: boolean) {
    const formData: FormData = new FormData();
    formData.set('username', usernameOrEmail);
    formData.set('password', password);
    formData.set('remember-me', rememberMe.toString());

    return this.http.post(`${this.loginUrl}`, formData, {
      responseType: 'text',
      params: {
        'remember-me': rememberMe
      }
    }).pipe(
        mergeMap(_ => {
          this.csrfService.clearCsrf();
          return this.currentUserService.refreshCurrentUser();
        }));
  }
}
