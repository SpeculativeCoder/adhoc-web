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
import {HttpClient} from "@angular/common/http";
import {StompService} from "../core/stomp.service";
import {Paging} from "../core/paging";
import {Observable} from "rxjs";
import {Page} from "../core/page";
import {Message} from "./message";

@Injectable({
  providedIn: 'root'
})
export class MessageService {

  private readonly messagesUrl: string;

  constructor(
    @Inject('BASE_URL') baseUrl: string,
    private http: HttpClient,
    private stomp: StompService
  ) {
    this.messagesUrl = `${baseUrl}/api/messages`;
  }

  getMessages(paging: Paging = new Paging()): Observable<Page<Message>> {
    return this.http.get<Page<Message>>(this.messagesUrl, {params: paging.toParams()});
  }

  getMessage(id: number): Observable<Message> {
    return this.http.get<Message>(`${this.messagesUrl}/${id}`);
  }

  updateMessage(message: Message): Observable<Message> {
    return this.http.put<Message>(`${this.messagesUrl}/${message.id}`, message);
  }
}
