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
import {Faction} from './faction';
import {Observable, of} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {map} from "rxjs/operators";
import {Paging} from "../shared/paging/paging";
import {Page} from "../shared/paging/page";

@Injectable({
  providedIn: 'root'
})
export class FactionService {

  private readonly factionsUrl: string;

  private cachedFactions: Faction[];

  constructor(@Inject('BASE_URL') baseUrl: string,
              private http: HttpClient) {
    this.factionsUrl = `${baseUrl}/api/factions`;
  }

  getFactions(paging: Paging = new Paging()): Observable<Page<Faction>> {
    return this.http.get<Page<Faction>>(this.factionsUrl, {params: paging.toParams()});
  }

  getFaction(id: number): Observable<Faction> {
    return this.http.get<Faction>(`${this.factionsUrl}/${id}`);
  }

  updateFaction(faction: Faction): Observable<Faction> {
    return this.http.put<Faction>(`${this.factionsUrl}/${faction.id}`, faction);
  }

  getCachedFactions(): Observable<Faction[]> {
    if (this.cachedFactions) {
      return of(this.cachedFactions);
    }
    return this.refreshCachedFactions();
  }

  getCachedFaction(id: number): Observable<Faction> {
    if (this.cachedFactions) {
      return of(this.cachedFactions.find(faction => faction.id === id));
    }
    return this.getFaction(id);
  }

  refreshCachedFactions(): Observable<Faction[]> {
    return this.http.get<Page<Faction>>(this.factionsUrl).pipe(
      map(factions => {
        this.cachedFactions ? this.cachedFactions.length = 0 : this.cachedFactions = [];
        this.cachedFactions.push(...factions.content);
        return this.cachedFactions;
      }));
  }
}
