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
import {StompService} from "../stomp/stomp.service";
import {MessageService} from "../messages/message.service";
import {Observable, of} from "rxjs";
import {map, tap} from "rxjs/operators";
import {User} from "../user/user";
import {Area} from "./area";

@Injectable({
  providedIn: 'root'
})
export class AreaService {

  private readonly areasUrl: string;

  private areas: Area[];

  constructor(@Inject('BASE_URL') baseUrl: string, private http: HttpClient, private stomp: StompService, private messages: MessageService) {
    this.areasUrl = `${baseUrl}/api/areas`;
  }

  getAreas(): Observable<Area[]> {
    if (this.areas) {
      return of(this.areas);
    }
    return this.refreshAreas();
  }

  refreshAreas(): Observable<Area[]> {
    return this.http.get<Area[]>(this.areasUrl).pipe(
      map(areas => {
        this.areas ? this.areas.length = 0 : this.areas = [];
        this.areas.push(...areas);
        return this.areas;
      }));
  }

  getArea(id: number): Observable<User> {
    return this.http.get<User>(`${this.areasUrl}/${id}`);
  }

  updateArea(area: Area): Observable<Area> {
    return this.http.put<Area>(`${this.areasUrl}/${area.id}`, area);
  }
}
