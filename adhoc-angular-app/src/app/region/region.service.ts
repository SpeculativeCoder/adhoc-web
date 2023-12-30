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
import {HttpClient} from "@angular/common/http";
import {StompService} from "../web/stomp.service";
import {MessageService} from "../web/message/message.service";
import {Region} from "./region";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";

@Injectable({
  providedIn: 'root'
})
export class RegionService {

  private readonly regionsUrl: string;

  private regions: Region[] = [];

  constructor(@Inject('BASE_URL') baseUrl: string, private http: HttpClient, private stomp: StompService, private messages: MessageService) {
    this.regionsUrl = `${baseUrl}/api/regions`;
  }

  getRegions(): Observable<Region[]> {
    return this.http.get<Region[]>(this.regionsUrl).pipe(
      map(regions => {
        this.regions ? this.regions.length = 0 : this.regions = [];
        this.regions.push(...regions);
        return this.regions;
      }));
  }

  getRegion(id: number): Observable<Region> {
    return this.http.get<Region>(`${this.regionsUrl}/${id}`);
  }

  updateRegion(region: Region): Observable<Region> {
    return this.http.put<Region>(`${this.regionsUrl}/${region.id}`, region);
  }
}
