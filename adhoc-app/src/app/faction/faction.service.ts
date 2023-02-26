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
import {Faction} from './faction';
import {Observable, of} from 'rxjs';
import {MessageService} from '../messages/message.service';
import {HttpClient} from '@angular/common/http';
import {map} from "rxjs/operators";

@Injectable({
  providedIn: 'root'
})
export class FactionService {

  private readonly factionsUrl: string;

  private factions: Faction[];

  constructor(@Inject('BASE_URL') baseUrl: string, private http: HttpClient, private messages: MessageService) {
    this.factionsUrl = `${baseUrl}/api/factions`;
  }

  getFactions(): Observable<Faction[]> {
    if (this.factions) {
      return of(this.factions);
    }
    return this.refreshFactions();
  }

  refreshFactions(): Observable<Faction[]> {
    return this.http.get<Faction[]>(this.factionsUrl).pipe(
      map(factions => {
        this.factions ? this.factions.length = 0 : this.factions = [];
        this.factions.push(...factions);
        return this.factions;
      }));
  }

  getFaction(id: number): Observable<Faction> {
    if (this.factions) {
      return of(this.factions.find(faction => faction.id === id));
    }
    return this.refreshFaction(id);
  }

  refreshFaction(id: number): Observable<Faction> {
    return this.http.get<Faction>(`${this.factionsUrl}/${id}`);
  }

  updateFaction(faction: Faction): Observable<Faction> {
    return this.http.put<Faction>(`${this.factionsUrl}/${faction.id}`, faction);
  }
}
