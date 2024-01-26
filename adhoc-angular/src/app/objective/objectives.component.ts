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

import {Component, OnInit} from '@angular/core';
import {ObjectiveService} from './objective.service';
import {Objective} from './objective';
import {FactionService} from '../faction/faction.service';
import {Faction} from '../faction/faction';
import {forkJoin} from 'rxjs';
import {SortEvent} from "../web/table-sort/header-sort.component";
import {User} from "../user/user";

@Component({
  selector: 'app-objectives',
  templateUrl: './objectives.component.html'
})
export class ObjectivesComponent implements OnInit {
  objectives: Objective[] = [];
  factions: Faction[] = [];

  constructor(private objectiveService: ObjectiveService, private factionService: FactionService) {
  }

  ngOnInit() {
    forkJoin([this.objectiveService.getObjectives(), this.factionService.getFactions()]).subscribe(data => {
      [this.objectives, this.factions] = data;
    });
  }

  getFaction(factionId: number): Faction {
    return this.factions.find(faction => faction.id === factionId);
  }

  changeFaction(objective: Objective, faction: Faction) {
    this.objectiveService.objectiveTaken(objective, faction);
  }

  sortBy(sort: SortEvent) {
    // console.log('sortBy');
    // console.log(sort);
    this.objectives.sort((a: User, b: User) => {
      const result = a[sort.column] < b[sort.column] ? -1 : a[sort.column] > b[sort.column] ? 1 : 0;
      return sort.direction === 'asc' ? result : -result;
    });
  }
}
