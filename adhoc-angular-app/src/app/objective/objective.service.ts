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
import {HttpClient} from '@angular/common/http';
import {MessageService} from '../messages/message.service';
import {Observable, of} from 'rxjs';
import {Objective} from './objective';
import {Faction} from '../faction/faction';
import {StompService} from '../stomp/stomp.service';
import {map} from 'rxjs/operators';
import {FactionService} from '../faction/faction.service';
import {CsrfService} from "../csrf/csrf.service";

@Injectable({
  providedIn: 'root'
})
export class ObjectiveService {

  private readonly objectivesUrl: string;

  private objectives: Objective[];

  constructor(
    @Inject('BASE_URL') baseUrl: string,
    private http: HttpClient,
    private csrfService: CsrfService,
    private messages: MessageService,
    private stomp: StompService,
    private factionService: FactionService
  ) {
    this.objectivesUrl = `${baseUrl}/api/objectives`;
    this.stomp
      .observeEvent('ObjectiveTaken')
      .subscribe((event: any) => this.handleObjectiveTaken(event.objectiveId, event.factionId));
  }

  getObjectives(): Observable<Objective[]> {
    if (this.objectives) {
      return of(this.objectives);
    }
    return this.refreshObjectives();
  }

  refreshObjectives(): Observable<Objective[]> {
    return this.http.get<Objective[]>(this.objectivesUrl).pipe(
      map(objectives => {
        this.objectives ? this.objectives.length = 0 : this.objectives = [];
        this.objectives.push(...objectives);
        return this.objectives;
      }));
  }

  getObjective(id: number): Observable<Objective> {
    return this.http.get<Objective>(`${this.objectivesUrl}/${id}`);
  }

  updateObjective(objective: Objective): Observable<Objective> {
    return this.http.put<Objective>(`${this.objectivesUrl}/${objective.id}`, objective);
  }

  objectiveTaken(objective: Objective, faction: Faction) {
    this.stomp.send('ObjectiveTaken', {
      objectiveId: objective.id,
      factionId: faction.id
    });
  }

  handleObjectiveTaken(objectiveId: number, factionId: number) {
    this.getObjectives().subscribe(objectives => {
      objectives.map(objective => {
        if (objective.id === objectiveId) {
          objective.factionId = factionId;
          this.factionService.getFaction(factionId).subscribe(faction => {
            this.messages.addMessage(`${objective.name} taken by ${faction.name}`);
          });
        }
      })
    });
  }
}
