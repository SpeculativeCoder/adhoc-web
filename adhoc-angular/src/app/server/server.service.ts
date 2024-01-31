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
import {HttpClient} from '@angular/common/http';
import {MessageService} from '../message/message.service';
import {Observable} from 'rxjs';
import {Server} from './server';
import {StompService} from '../core/stomp.service';
import {map} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ServerService {

  private readonly serversUrl: string;

  private servers: Server[] = [];

  constructor(
    @Inject('BASE_URL') baseUrl: string,
    private http: HttpClient,
    private messages: MessageService,
    private stomp: StompService
  ) {
    this.serversUrl = `${baseUrl}/api/servers`;
  }

  getServers(): Observable<Server[]> {
    return this.http.get<Server[]>(this.serversUrl).pipe(
      map(servers => {
        this.servers ? this.servers.length = 0 : this.servers = [];
        this.servers.push(...servers);
        return this.servers;
      }));
  }

  getServer(id: number): Observable<Server> {
    return this.http.get<Server>(`${this.serversUrl}/${id}`);
  }

  updateServer(server: Server): Observable<Server> {
    return this.http.put<Server>(`${this.serversUrl}/${server.id}`, server);
  }

}
