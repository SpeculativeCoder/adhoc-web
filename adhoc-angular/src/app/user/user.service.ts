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
import {BehaviorSubject, mergeMap, Observable, of, take} from 'rxjs';
import {StompService} from '../core/stomp.service';
import {UserRegisterRequest} from "./request-response/user-register-request";
import {Paging} from "../shared/paging/paging";
import {Page} from "../shared/paging/page";
import {CsrfService} from "../core/csrf.service";
import {UserNavigateResponse} from "./request-response/user-navigate-response";

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private readonly usersUrl: string;
  private readonly loginUrl: string;

  private currentUser$: BehaviorSubject<User>;

  constructor(@Inject('BASE_URL') baseUrl: string,
              private http: HttpClient,
              private csrfService: CsrfService,
              private stomp: StompService) {

    this.usersUrl = `${baseUrl}/api/users`;
    this.loginUrl = `${baseUrl}/login`;

    this.stomp
      .observeEvent('UserDefeatedUser')
      .subscribe((body: any) => this.handleUserDefeatedUser(body['userId'], body['userHuman'], body['defeatedUserId'], body['defeatedUserHuman']));
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

  getCurrentUser(): Observable<User> {
    return this.getCurrentUser$().pipe(take(1));
  }

  getCurrentUser$(): Observable<User> {
    if (this.currentUser$) {
      return this.currentUser$;
    }
    return this.refreshCurrentUser$();
  }

  refreshCurrentUser(): Observable<User> {
    return this.refreshCurrentUser$().pipe(take(1));
  }

  refreshCurrentUser$() {
    return this.http.get<User>(`${this.usersUrl}/current`).pipe(
      mergeMap(user => {
        if (this.currentUser$) {
          this.currentUser$.next(user);
        } else {
          this.currentUser$ = new BehaviorSubject(user);
        }
        this.csrfService.clearCsrf();
        return this.currentUser$;
      }));
  }

  register(userRegisterRequest: UserRegisterRequest): Observable<User> {
    return this.http.post(`${this.usersUrl}/register`, {...userRegisterRequest}, {
      params: {
        'remember-me': userRegisterRequest.rememberMe || false
      }
    }).pipe(
      mergeMap(user => {
        if (this.currentUser$) {
          this.currentUser$.next(user);
        } else {
          this.currentUser$ = new BehaviorSubject(user);
        }
        this.csrfService.clearCsrf();
        return this.currentUser$;
      }));
  }

  getCurrentUserOrRegister(): Observable<User> {
    return this.getCurrentUser().pipe(
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
      mergeMap(_ => this.refreshCurrentUser()));
  }

  navigateCurrentUser(destinationServerId?: number): Observable<UserNavigateResponse> {
    return this.http.post(`${this.usersUrl}/current/navigate`, {destinationServerId: destinationServerId});
  }

  userDefeatedUser(user: User, defeatedUser?: User) {
    this.stomp.send('UserDefeatedUser', {userId: null /*user.id*/, defeatedUserId: defeatedUser?.id || null});
  }

  handleUserDefeatedUser(userId: number, userHuman: boolean, defeatedUserId: number, defeatedUserHuman: boolean) {
    // let user: User;
    // let defeatedUser: User;
    // this.users.map(user => {
    //   if (user.id === userId) {
    //     user = user;
    //   }
    //   if (user.id === defeatedUserId) {
    //     defeatedUser = user;
    //   }
    // });
    // user.score++;

    // TODO
    if (userHuman || defeatedUserHuman) {
      console.log(`User ${userId} defeated user ${defeatedUserId}`);
    }
  }
}
