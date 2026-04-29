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
import {forkJoin} from 'rxjs';
import {Server} from './server';
import {ServerService} from './server.service';
import {MetaService} from "../core/meta.service";
import {TableHeaderSortableComponent} from "../shared/table/table-header-sortable.component";
import {Router, RouterLink} from "@angular/router";
import {CommonModule} from "@angular/common";
import {TableSortDirective} from "../shared/table/table-sort.directive";
import {Page} from "../shared/page";
import {Sort} from "../shared/sort";
import {CurrentUser} from '../user/current/current-user';
import {CurrentUserService} from '../user/current/current-user.service';
import {Paging} from '../shared/paging';
import {TableComponent} from '../shared/table/table.component';
import {Pagination} from '../shared/pagination/pagination.component';

@Component({
  selector: 'app-servers',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    TableSortDirective,
    TableHeaderSortableComponent,
    TableComponent,
    Pagination
  ],
  templateUrl: './servers.component.html'
})
export class ServersComponent implements OnInit {

  protected featureFlags = signal('');

  protected servers = signal<Page<Server> | undefined>(undefined);

  protected currentUser = signal<CurrentUser | null>(null);

  private paging: Paging = new Paging();

  constructor(private metaService: MetaService,
              private serverService: ServerService,
              private currentUserService: CurrentUserService,
              private router: Router) {
  }

  ngOnInit() {
    this.featureFlags.set(this.metaService.getFeatureFlags());

    this.refreshData();
  }

  private refreshData() {
    forkJoin([
      this.serverService.getServers(this.paging),
      this.currentUserService.getCurrentUser(),
    ]).subscribe(data => {
      this.servers.set(data[0]);
      this.currentUser.set(data[1]);
    });
  }

  joinServer(server: Server) {
    this.router.navigate(['client'], {
      state: {
        serverId: server.id
      }
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
