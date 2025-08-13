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
import {forkJoin} from 'rxjs';
import {Server} from './server';
import {ServerService} from './server.service';
import {MetaService} from "../system/meta.service";
import {HeaderSortComponent} from "../shared/table-sort/header-sort.component";
import {Router, RouterLink} from "@angular/router";
import {CommonModule} from "@angular/common";
import {TableSortDirective} from "../shared/table-sort/table-sort.directive";
import {Page} from "../shared/paging/page";
import {Paging} from "../shared/paging/paging";
import {Sort} from "../shared/paging/sort";
import {NgbPagination} from "@ng-bootstrap/ng-bootstrap";
import {RegisterService} from '../user/register/register.service';
import {NavigateService} from '../user/navigate/navigate.service';

@Component({
  selector: 'app-servers',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    TableSortDirective,
    HeaderSortComponent,
    NgbPagination
  ],
  templateUrl: './servers.component.html'
})
export class ServersComponent implements OnInit {

  servers: Page<Server> = new Page();
  private paging: Paging = new Paging();

  constructor(private serverService: ServerService,
              private registerService: RegisterService,
              private navigateService: NavigateService,
              private metaService: MetaService,
              private router: Router) {
  }

  ngOnInit() {
    this.refreshServers();
  }

  private refreshServers() {
    forkJoin([
      this.serverService.getServers(this.paging)
    ]).subscribe(data => {
      [this.servers] = data;
    });
  }

  joinServer(server: Server) {
    this.router.navigate(['client'], {
      state: {
        serverId: server.id
      }
    });
  }

  onPageChange(pageIndex: number) {
    this.paging.page = pageIndex;
    this.refreshServers();
  }

  onSort(sort: Sort) {
    this.paging.sort = [new Sort(sort.column, sort.direction)];
    this.refreshServers();
  }
}
