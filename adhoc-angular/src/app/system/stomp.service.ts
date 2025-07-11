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

import {Injectable} from '@angular/core';
import webstomp, {Client, Message} from 'webstomp-client';
import {Observable, Subject} from 'rxjs';
import SockJS from "sockjs-client";
import {CsrfService} from "./csrf.service";

@Injectable({
  providedIn: 'root'
})
export class StompService {

  private client?: Client;
  private eventListeners: { [key: string]: Subject<object> } = {};

  constructor(private csrfService: CsrfService) {
  }

  connect() {
    // const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    // this.client = webstomp.client(protocol + '//' + location.host + '/stomp/user', {
    //   debug: false,
    //     // heartbeat: false,
    //     // heartbeat: { outgoing: 5000, incoming: 5000 },
    //     protocols: ['v12.stomp']
    // });

    this.csrfService.getCsrf().subscribe(csrf => {
      this.client = webstomp.over(new SockJS(window.location.protocol + '//' + location.host + '/ws/stomp/user_sockjs', {}), {
        debug: false,
      });
      let headers: { [key: string]: string } = {}
      headers[csrf!.headerName!] = csrf!.token!;
      this.client.connect(headers, () => this.onConnect(), () => this.onError());
    });
  }

  disconnect() {
    this.eventListeners = {}; // TODO
    if (this.client && this.client.connected) {
      this.client.disconnect(() => this.onDisconnect());
    }
  }

  private onConnect() {
    this.client!.subscribe('/topic/events', message => this.onMessage(message));
  }

  send(eventType: string, payload: object) {
    this.client!.send(`/app/${eventType}`, JSON.stringify(payload));
  }

  observeEvent(eventType: string): Observable<object> {
    return this.eventListeners[eventType] || (this.eventListeners[eventType] = new Subject());
  }

  private onMessage(message: Message) {
    const event = JSON.parse(message.body);
    // let services know the event happened so they can update cached data etc.
    const eventListener = this.eventListeners[event['eventType']];
    if (eventListener) {
      eventListener.next(event);
    } else {
      //console.log("no event listener for " + body.eventType);
    }
  }

  private onError() {
    // TODO
  }

  private onDisconnect() {
    // TODO
  }
}
