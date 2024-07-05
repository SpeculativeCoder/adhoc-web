/*
 * Copyright (c) 2022-2024 SpeculativeCoder (https://github.com/SpeculativeCoder)
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
import {UserNavigateRequest} from "./request-response/user-navigate-request";
import {CsrfService} from "../core/csrf.service";

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private readonly usersUrl: string;
  private readonly loginUrl: string;

  private currentUser$: BehaviorSubject<User> = new BehaviorSubject(null);

  constructor(@Inject('BASE_URL') baseUrl: string,
              private http: HttpClient,
              private csrfService: CsrfService,
              private stomp: StompService) {

    this.usersUrl = `${baseUrl}/api/users`;
    this.loginUrl = `${baseUrl}/login`;

    this.stomp
      .observeEvent('UserDefeatedUser')
      .subscribe((body: any) => this.handleUserDefeatedUser(body['userId'], body['defeatedUserId']));
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
    if (this.currentUser$.value) {
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
        this.currentUser$.next(user);
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
        this.currentUser$.next(user);
        this.csrfService.clearCsrf();
        return of(this.currentUser$.value);
      }));
  }

  getCurrentUserOrRegister(): Observable<User> {
    return this.currentUser$.value
      ? of(this.currentUser$.value)
      : this.register({
        human: true,
        // regionId: regionId,
        // serverId: serverId,
      });
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

  navigateCurrentUser(userNavigateRequest: UserNavigateRequest): Observable<User> {
    return this.http.post(`${this.usersUrl}/current/navigate`,
      {...userNavigateRequest}).pipe(
      mergeMap(user => {
        this.currentUser$.next(user);
        this.csrfService.clearCsrf();
        return of(this.currentUser$.value);
      }));
  }

  navigateCurrentUserOrRegister(regionId: number, serverId: number): Observable<User> {
    return this.currentUser$.value
      ? this.navigateCurrentUser({
        regionId: regionId,
        serverId: serverId
      })
      : this.register({
        human: true,
        regionId: regionId,
        serverId: serverId
      });
    ;
  }

  userDefeatedUser(user: User, defeatedUser: User) {
    this.stomp.send('UserDefeatedUser', {userId: user.id, defeatedUserId: defeatedUser.id});
  }

  handleUserDefeatedUser(userId: number, defeatedUserId: number) {
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
    console.log(`User ${userId} defeated user ${defeatedUserId}`);
  }
}
