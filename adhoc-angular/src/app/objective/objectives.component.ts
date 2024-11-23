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
import {FactionService} from '../faction/faction.service';
import {Faction} from '../faction/faction';
import {forkJoin} from 'rxjs';
import {HeaderSortComponent} from "../shared/table-sort/header-sort.component";
import {CommonModule} from "@angular/common";
import {RouterLink} from "@angular/router";
import {TableSortDirective} from "../shared/table-sort/table-sort.directive";
import {Page} from "../shared/paging/page";
import {Paging} from "../shared/paging/paging";
import {Sort} from "../shared/paging/sort";
import {Objective} from "./objective";
import {NgbPagination} from "@ng-bootstrap/ng-bootstrap";

@Component({
  selector: 'app-objectives',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    TableSortDirective,
    HeaderSortComponent,
    NgbPagination
  ],
  templateUrl: './objectives.component.html'
})
export class ObjectivesComponent implements OnInit {

  objectives: Page<Objective> = new Page();
  private paging: Paging = new Paging();

  factions: Faction[] = [];

  constructor(private objectiveService: ObjectiveService,
              private factionService: FactionService) {
  }

  ngOnInit() {
    this.refreshObjectives();
  }

  private refreshObjectives() {
    forkJoin([
      this.objectiveService.getObjectives(this.paging),
      this.factionService.getCachedFactions()
    ]).subscribe(data => {
      [this.objectives, this.factions] = data;
    });
  }

  getFaction(factionId: number): Faction {
    return this.factions.find(faction => faction.id === factionId);
  }

  onPageChange(pageIndex: number) {
    this.paging.page = pageIndex;
    this.refreshObjectives();
  }

  onSort(sort: Sort) {
    this.paging.sort = [new Sort(sort.column, sort.direction)];
    this.refreshObjectives();
  }
}
