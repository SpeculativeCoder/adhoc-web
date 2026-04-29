/*
 * Copyright (c) 2022-2026 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

import {ChangeDetectionStrategy, Component, OnInit, signal} from '@angular/core';
import {forkJoin} from "rxjs";
import {RegionService} from "./region.service";
import {TableHeaderSortableComponent} from "../shared/table/table-header-sortable.component";
import {CommonModule} from "@angular/common";
import {RouterLink} from "@angular/router";
import {TableSortDirective} from "../shared/table/table-sort.directive";
import {Page} from "../shared/page";
import {Paging} from "../shared/paging";
import {Sort} from "../shared/sort";
import {CurrentUser} from '../user/current/current-user';
import {MetaService} from '../core/meta.service';
import {CurrentUserService} from '../user/current/current-user.service';
import {Region} from './region';
import {TableComponent} from '../shared/table/table.component';
import {Pagination} from '../shared/pagination/pagination.component';

@Component({
  selector: 'app-regions',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    TableSortDirective,
    TableHeaderSortableComponent,
    TableComponent,
    Pagination
  ],
  templateUrl: './regions.component.html'
})
export class RegionsComponent implements OnInit {

  protected featureFlags = signal('');

  protected regions = signal<Page<Region> | undefined>(undefined);

  protected currentUser = signal<CurrentUser | null>(null);

  private paging: Paging = new Paging();

  constructor(private metaService: MetaService,
              private regionService: RegionService,
              private currentUserService: CurrentUserService) {
  }

  ngOnInit() {
    this.featureFlags.set(this.metaService.getFeatureFlags());

    this.refreshData();
  }

  private refreshData() {
    forkJoin([
      this.regionService.getRegions(this.paging),
      this.currentUserService.getCurrentUser(),
    ]).subscribe(data => {
      this.regions.set(data[0]);
      this.currentUser.set(data[1]);
    });
  }

  onPageChanged(pageIndex: number) {
    this.paging.page = pageIndex;
    this.refreshData();
  }

  onSort(sort: Sort) {
    this.paging.sort = [new Sort(sort.column, sort.direction)];
    this.refreshData();
  }
}
