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
import {MessageService} from '../web/message/message.service';
import {BehaviorSubject, mergeMap, Observable, of, take} from 'rxjs';
import {StompService} from '../web/stomp.service';
import {map} from 'rxjs/operators';
import {PropertiesService} from "../properties/properties.service";
import {Router} from "@angular/router";
import {UserRegisterRequest} from "./user-register-request";

@Injectable({
  providedIn: 'root'
})
export class UserService {

  private readonly usersUrl: string;
  private readonly loginUrl: string;

  private users: User[];
  private currentUser$: BehaviorSubject<User> = new BehaviorSubject(null);

  constructor(@Inject('BASE_URL') baseUrl: string,
              private http: HttpClient,
              private configService: PropertiesService,
              private stomp: StompService,
              private messages: MessageService,
              private router: Router) {

    this.usersUrl = `${baseUrl}/api/users`;
    this.loginUrl = `${baseUrl}/login`;

    this.stomp.observeEvent('UserDefeatedUser').subscribe(body => this.handleUserDefeatedUser(body['userId'], body['defeatedUserId']));
    this.stomp.observeEvent('UserDefeatedBot').subscribe(body => this.handleUserDefeatedBot(body['userId']));
    this.stomp
      .observeEvent('FactionScoring')
      .subscribe((body: any) => this.handleFactionScoring(body['factionAwardedScores']));
  }

  getUsers(): Observable<User[]> {
    if (this.users) {
      return of(this.users);
    }
    return this.refreshUsers();
  }

  refreshUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.usersUrl).pipe(
      map(users => {
        this.users ? this.users.length = 0 : this.users = [];
        this.users.push(...users);
        return this.users;
      }));
  }

  getUser(id: number): Observable<User> {
    return this.http.get<User>(`${this.usersUrl}/${id}`);
  }

  getCurrentUser$(): Observable<User> {
    if (this.currentUser$.value) {
      return this.currentUser$;
    }
    return this.refreshCurrentUser$();
  }

  refreshCurrentUser$() {
    return this.http.get<User>(`${this.usersUrl}/current`, {withCredentials: true}).pipe(
      mergeMap(user => {
        this.currentUser$.next(user);
        return this.currentUser$;
      }));
  }

  getCurrentUser(): Observable<User> {
    if (this.currentUser$.value) {
      return of(this.currentUser$.value);
    }
    return this.refreshCurrentUser();
  }

  refreshCurrentUser(): Observable<User> {
    return this.refreshCurrentUser$().pipe(take(1));
  }

  register(userRegisterRequest: UserRegisterRequest): Observable<User> {
    return this.http.post(`${this.usersUrl}/register`, {...userRegisterRequest}, {
      withCredentials: true,
      params: {
        'remember-me': userRegisterRequest.rememberMe || false
      }}).pipe(
      mergeMap(user => {
        this.currentUser$.next(user);
        return of(this.currentUser$.value);
      }));
  }

  getCurrentUserOrRegister(serverId: number): Observable<User> {
    return this.currentUser$.value ? of(this.currentUser$.value)
      : this.register({
        serverId: serverId,
        human: true
      });
  }

  login(usernameOrEmail: string, password: string, rememberMe: boolean) {
    const formData: FormData = new FormData();
    formData.set('username', usernameOrEmail);
    formData.set('password', password);
    formData.set('remember-me', rememberMe.toString());

    return this.http.post(`${this.loginUrl}`, formData, {
      responseType: 'text',
      withCredentials: true,
      params: {
        'remember-me': rememberMe
      }
    }).pipe(
      mergeMap(_ => this.refreshCurrentUser()));
  }

  updateUser(user: User): Observable<User> {
    return this.http.put<User>(`${this.usersUrl}/${user.id}`, user);
  }

  userDefeatedUser(user: User, defeatedUser: User) {
    this.stomp.send('UserDefeatedUser', {userId: user.id, defeatedUserId: defeatedUser.id});
  }

  userDefeatedBot(user: User) {
    this.stomp.send('UserDefeatedBot', {userId: user.id});
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
    // this.messages.add(`${user.name} killed ${defeatedUser.name}`);
    this.messages.addMessage(`User ${userId} killed user ${defeatedUserId}`);
  }

  handleUserDefeatedBot(userId: number) {
    // let user: User;
    // this.users.map(user => {
    //   if (user.id === userId) {
    //     user = user;
    //   }
    // });
    // user.score++;
    // this.messages.add(`${user.name} killed a bot`);
    this.messages.addMessage(`User ${userId} killed a bot`);
  }

  handleFactionScoring(factionIdToAwardedScore: any) {
    console.log(factionIdToAwardedScore);
    for (const factionId of Object.keys(factionIdToAwardedScore)) {
      console.log(`${factionId} = ${factionIdToAwardedScore[factionId]}`);
    }
    // TODO: announce score
    //this.messages.add(JSON.stringify(factionIdToAwardedScore));
  }
}
