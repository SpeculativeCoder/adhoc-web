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

import {Component, OnInit} from '@angular/core';
import {Objective} from './objective';
import {ActivatedRoute, Router} from '@angular/router';
import {ObjectiveService} from './objective.service';
import {FactionService} from '../faction/faction.service';
import {Faction} from '../faction/faction';
import {forkJoin} from 'rxjs';
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-objective',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './objective.component.html'
})
export class ObjectiveComponent implements OnInit {

  objective: Objective = {};
  factions: Faction[] = [];

  constructor(private route: ActivatedRoute,
              private objectiveService: ObjectiveService,
              private factionService: FactionService,
              private router: Router) {
  }

  ngOnInit() {
    const objectiveId = +this.route.snapshot.paramMap.get('id');
    forkJoin([this.objectiveService.getObjective(objectiveId), this.factionService.getCachedFactions()]).subscribe(data => {
      [this.objective, this.factions] = data;
    });
  }

  getFaction(factionId: number): Faction {
    return this.factions.find(faction => faction.id === factionId);
  }

  save() {
    this.objectiveService.updateObjective(this.objective).subscribe(objective => {
      this.objective = objective;
    });
  }

  back() {
    this.router.navigateByUrl('/objectives');
  }
}
