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
import {forkJoin} from "rxjs";
import {RegionService} from "./region.service";
import {HeaderSortComponent} from "../shared/table-sort/header-sort.component";
import {CommonModule} from "@angular/common";
import {RouterLink} from "@angular/router";
import {TableSortDirective} from "../shared/table-sort/table-sort.directive";
import {Page} from "../shared/paging/page";
import {Paging} from "../shared/paging/paging";
import {Sort} from "../shared/paging/sort";
import {Region} from "./region";
import {NgbPagination} from "@ng-bootstrap/ng-bootstrap";

@Component({
  selector: 'app-regions',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    TableSortDirective,
    HeaderSortComponent,
    NgbPagination
  ],
  templateUrl: './regions.component.html'
})
export class RegionsComponent implements OnInit {

  regions: Page<Region> = new Page();
  private paging: Paging = new Paging();

  constructor(private regionService: RegionService) {
  }

  ngOnInit() {
    this.refreshRegions();
  }

  private refreshRegions() {
    forkJoin([
      this.regionService.getRegions(this.paging)
    ]).subscribe(data => {
      [this.regions] = data;
    });
  }

  onPageChange(pageIndex: number) {
    this.paging.page = pageIndex;
    this.refreshRegions();
  }

  onSort(sort: Sort) {
    this.paging.sort = [new Sort(sort.column, sort.direction)];
    this.refreshRegions();
  }
}
