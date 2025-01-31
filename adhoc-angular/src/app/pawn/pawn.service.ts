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
import {Pawn} from "./pawn";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {User} from "../user/user";
import {Page} from "../shared/paging/page";
import {Paging} from "../shared/paging/paging";

@Injectable({
  providedIn: 'root'
})
export class PawnService {

  private readonly pawnsUrl: string;

  constructor(@Inject('BASE_URL') baseUrl: string,
              private http: HttpClient) {
    this.pawnsUrl = `${baseUrl}/api/pawns`;
  }

  getPawns(paging: Paging = new Paging()): Observable<Page<Pawn>> {
    return this.http.get<Page<Pawn>>(this.pawnsUrl, {params: paging.toParams()});
  }

  getPawn(id: number): Observable<User> {
    return this.http.get<User>(`${this.pawnsUrl}/${id}`);
  }

  updatePawn(pawn: Pawn): Observable<Pawn> {
    return this.http.put<Pawn>(`${this.pawnsUrl}/${pawn.id}`, pawn);
  }
}
