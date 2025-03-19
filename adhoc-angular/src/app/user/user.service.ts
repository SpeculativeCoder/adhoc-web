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
import {mergeMap, Observable, of} from 'rxjs';
import {UserRegisterRequest} from "./request-response/user-register-request";
import {Paging} from "../shared/paging/paging";
import {Page} from "../shared/paging/page";
import {CsrfService} from "../system/csrf.service";
import {CurrentUserService} from './current-user.service';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private readonly usersUrl: string;
  private readonly loginUrl: string;

  constructor(@Inject('BASE_URL') baseUrl: string,
              private http: HttpClient,
              private currentUserService: CurrentUserService,
              private csrfService: CsrfService) {

    this.usersUrl = `${baseUrl}/api/users`;
    this.loginUrl = `${baseUrl}/api/login`;
  }

  getUsers(paging: Paging = new Paging()): Observable<Page<User>> {
    return this.http.get<Page<User>>(this.usersUrl, {params: paging.toParams()});
  }

  getUser(id: number): Observable<User> {
    return this.http.get<User>(`${this.usersUrl}/${id}`);
  }

  updateUser(user: User): Observable<User> {
    return this.http.put<User>(`${this.usersUrl}/${user.id}`, user);
  }

  register(userRegisterRequest: UserRegisterRequest): Observable<User> {
    return this.http.post(`${this.usersUrl}/register`, {...userRegisterRequest}, {
      params: {
        'remember-me': userRegisterRequest.rememberMe || false
      }
    }).pipe(
      mergeMap(user => {
        this.csrfService.clearCsrf();
        this.currentUserService.setCurrentUser(user);
        return of(user);
      }));
  }

  getCurrentUserOrRegister(): Observable<User> {
    return this.currentUserService.getCurrentUser().pipe(
      mergeMap(currentUser => {
        return currentUser ? of(currentUser) : this.register({
          human: true
        });
      }));
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
