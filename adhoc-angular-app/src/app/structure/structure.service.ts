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
import {Structure} from "../structure/structure";
import {HttpClient} from "@angular/common/http";
import {MessageService} from "../web/message/message.service";
import {StompService} from "../web/stomp.service";
import {Observable} from "rxjs";
import {map} from "rxjs/operators";

@Injectable({
  providedIn: 'root'
})
export class StructureService {

  private readonly structuresUrl: string;

  private structures: Structure[] = [];

  constructor(
    @Inject('BASE_URL') baseUrl: string,
    private http: HttpClient,
    private messages: MessageService,
    private stomp: StompService
  ) {
    this.structuresUrl = `${baseUrl}/api/structures`;
  }

  getStructures(): Observable<Structure[]> {
    return this.http.get<Structure[]>(this.structuresUrl).pipe(
      map(structures => {
        this.structures ? this.structures.length = 0 : this.structures = [];
        this.structures.push(...structures);
        return this.structures;
      }));
  }

  getStructure(id: number): Observable<Structure> {
    return this.http.get<Structure>(`${this.structuresUrl}/${id}`);
  }

  updateStructure(structure: Structure): Observable<Structure> {
    return this.http.put<Structure>(`${this.structuresUrl}/${structure.id}`, structure);
  }
}
