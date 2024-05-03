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
import {StructureService} from './structure.service';
import {Structure} from './structure';
import {Faction} from '../faction/faction';
import {FactionService} from '../faction/faction.service';
import {forkJoin} from 'rxjs';
import {SortEvent} from '../shared/table-sort/header-sort.component';
import {CommonModule} from "@angular/common";
import {RouterLink} from "@angular/router";
import {SimpleDatePipe} from "../shared/simple-date/simple-date.pipe";
import {TableSortDirective} from "../shared/table-sort/table-sort.directive";

@Component({
  selector: 'app-structures',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    SimpleDatePipe,
    TableSortDirective
  ],
  templateUrl: './structures.component.html'
})
export class StructuresComponent implements OnInit {
  structures: Structure[] = [];
  factions: Faction[] = [];

  constructor(private structureService: StructureService, private factionService: FactionService) {
  }

  getFaction(factionId: number): Faction {
    return this.factions.find(faction => faction.id === factionId);
  }

  ngOnInit() {
    forkJoin([this.factionService.getFactions(), this.structureService.getStructures()]).subscribe(data => {
      [this.factions, this.structures] = data;
    });
  }

  sortBy(sort: SortEvent) {
    // console.log('sortBy');
    // console.log(sort);
    this.structures.sort((a: any, b: any) => {
      const result = a[sort.column] < b[sort.column] ? -1 : a[sort.column] > b[sort.column] ? 1 : 0;
      return sort.direction === 'asc' ? result : -result;
    });
  }
}
