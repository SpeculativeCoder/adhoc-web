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
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {User} from "../user/user";
import {Area} from "./area";
import {Paging} from "../shared/paging/paging";
import {Page} from "../shared/paging/page";

@Injectable({
  providedIn: 'root'
})
export class AreaService {

  private readonly areasUrl: string;

  constructor(@Inject('BASE_URL') baseUrl: string,
              private http: HttpClient) {
    this.areasUrl = `${baseUrl}/adhoc_api/areas`;
  }

  getAreas(paging: Paging = new Paging()): Observable<Page<Area>> {
    return this.http.get<Page<Area>>(this.areasUrl, {params: paging.toParams()});
  }

  getArea(id: number): Observable<User> {
    return this.http.get<User>(`${this.areasUrl}/${id}`);
  }

  updateArea(area: Area): Observable<Area> {
    return this.http.put<Area>(`${this.areasUrl}/${area.id}`, area);
  }
}
