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
import {UserRegisterRequest} from './user-register-request';
import {mergeMap, of} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {CurrentUserService} from '../current/current-user.service';
import {CsrfService} from '../../system/csrf.service';
import {UserRegisterResponse} from './user-register-response';

@Injectable({
  providedIn: 'root'
})
export class RegisterService {

  private readonly usersUrl: string;

  constructor(@Inject('BASE_URL') baseUrl: string,
              private http: HttpClient,
              private currentUserService: CurrentUserService,
              private csrfService: CsrfService) {

    this.usersUrl = `${baseUrl}/adhoc_api/users`;
  }

  register(userRegisterRequest: UserRegisterRequest) {
    return this.http.post(`${this.usersUrl}/register`, {...userRegisterRequest}, {
      params: {
        'remember-me': userRegisterRequest.rememberMe || false
      }
    }).pipe(
        mergeMap((user: UserRegisterResponse) => {
          this.csrfService.clearCsrf();
          return this.currentUserService.refreshCurrentUser();
        }));
  }

  getCurrentUserOrRegister() {
    return this.currentUserService.getCurrentUser().pipe(
        mergeMap(currentUser => {
          return currentUser ? of(currentUser) : this.register({
            human: true
          });
        }));
  }
}
