/*
 * Copyright (c) 2022-2023 SpeculativeCoder (https://github.com/SpeculativeCoder)
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
import {MessageService} from '../messages/message.service';
import {BehaviorSubject, mergeMap, Observable, of, Subject} from 'rxjs';
import {StompService} from '../stomp/stomp.service';
import {map} from 'rxjs/operators';
import {Server} from "../server/server";
import {ConfigService} from "../config/config.service";
import {Router} from "@angular/router";

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly usersUrl: string;
  private readonly loginUrl: string;

  private users: User[];
  //private currentUser: User;
  private currentUser$: BehaviorSubject<User> = new BehaviorSubject(null);

  constructor(@Inject('BASE_URL') baseUrl: string,
              private http: HttpClient,
              private configService: ConfigService,
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

  getCurrentUser(): Observable<User> {
    if (this.currentUser$.value) {
      return of(this.currentUser$.value);
    }
    return this.refreshCurrentUser();
  }

  watchCurrentUser(): Observable<User> {
    if (this.currentUser$.value) {
      return this.currentUser$;
    }
    return this.refreshCurrentUser();
  }

  refreshCurrentUser(): Observable<User> {
    return this.http.get<User>(`${this.usersUrl}/current`, {withCredentials: true}).pipe(
      mergeMap(user => {
        this.currentUser$.next(user);
        return this.currentUser$;
      }));
  }

  register(user: User): Observable<User> {
    return this.http.post(`${this.usersUrl}/register`, {...user}, {withCredentials: true}).pipe(
      mergeMap(user => {
        this.currentUser$.next(user);
        return of(this.currentUser$.value);
      }));
  }

  // autoRegister(serverId: number): Observable<User> {
  //   return this.http.post(`${this.usersUrl}/autoRegister`, { serverId: serverId }, {withCredentials: true})
  //       .pipe(tap(user => this.currentUser = user));
  // }

  login(usernameOrEmail: string, password: string) {
    const formData: FormData = new FormData();
    formData.set('username', usernameOrEmail ?? '');
    formData.set('password', password ?? '');

    return this.http.post(`${this.loginUrl}`, formData, {responseType: 'text', withCredentials: true}).pipe(
      mergeMap(result => this.getCurrentUser()));
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

  joinServer(server: Server) {
    let observable: Observable<User> =
      this.currentUser$.value
        ? of(this.currentUser$.value)
        : this.register({serverId: server.id});

    observable.subscribe(user => {
      let unrealEngineCommandLine = server.publicIp + ":" + server.publicWebSocketPort;

      //if (server.publicWebSocketPort != 8889) {
      //   unrealEngineCommandLine += ":" + server.publicWebSocketPort;
      //}
      //unrealEngineCommandLine += '?WebSocketPort=' + server.publicWebSocketPort;

      unrealEngineCommandLine += ''; // '?ServerID=' + server.id;
      //unrealEngineCommandLine += '?ServerDomain=' + this.configService.serverDomain;
      if (user && user.id && user.token) {
        unrealEngineCommandLine += '?UserID=' + user.id + '?Token=' + user.token;
      }
      if (this.configService.featureFlags) {
        unrealEngineCommandLine += ' FeatureFlags=' + this.configService.featureFlags;
      }
      unrealEngineCommandLine += ' -stdout';
      console.log('unrealEngineCommandLine: ' + unrealEngineCommandLine);

      window.sessionStorage.setItem('UnrealEngine_CommandLine', unrealEngineCommandLine);

      // let webSocketProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      // let webSocketUrl = webSocketProtocol + '//' + server.id + '-' + this.configService.serverDomain + ':' + server.publicWebSocketPort;
      // console.log('webSocketUrl: '+webSocketUrl);

      if (server.webSocketUrl) {
        console.log('webSocketUrl: ' + server.webSocketUrl);
        window.sessionStorage.setItem('UnrealEngine_WebSocketUrl', server.webSocketUrl);
      } else {
        window.sessionStorage.removeItem('UnrealEngine_WebSocketUrl');
      }

      //let clientUrl = location.protocol + '//' + location.host + '/' + server.mapName + '/Client.html';
      //console.log('clientUrl: ' + clientUrl);

      // console.log('angular navigate to ' + '/client/' + server.mapName);
      this.router.navigate(['/client/' + server.mapName]);

      //window.location.href = clientUrl;
    });
  }
}
