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
import {Pawn} from "./pawn";
import {HttpClient} from "@angular/common/http";
import {StompService} from "../web/stomp.service";
import {MessageService} from "../web/message/message.service";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";
import {User} from "../user/user";

@Injectable({
  providedIn: 'root'
})
export class PawnService {

  private readonly pawnsUrl: string;

  private pawns: Pawn[];

  constructor(@Inject('BASE_URL') baseUrl: string, private http: HttpClient, private stomp: StompService, private messages: MessageService) {
    this.pawnsUrl = `${baseUrl}/api/pawns`;
  }

  getPawns(): Observable<Pawn[]> {
    return this.http.get<Pawn[]>(this.pawnsUrl).pipe(
      map(pawns => {
        this.pawns ? this.pawns.length = 0 : this.pawns = [];
        this.pawns.push(...pawns);
        return this.pawns;
      }));
  }

  getPawn(id: number): Observable<User> {
    return this.http.get<User>(`${this.pawnsUrl}/${id}`);
  }

  updatePawn(pawn: Pawn): Observable<Pawn> {
    return this.http.put<Pawn>(`${this.pawnsUrl}/${pawn.id}`, pawn);
  }
}
