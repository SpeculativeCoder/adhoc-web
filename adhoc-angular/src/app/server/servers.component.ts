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

import {UserService} from '../user/user.service';
import {Component, OnInit} from '@angular/core';
import {forkJoin} from 'rxjs';
import {Server} from './server';
import {ServerService} from './server.service';
import {PropertiesService} from "../properties/properties.service";
import {HeaderSortComponent, SortEvent} from "../shared/table-sort/header-sort.component";
import {Router, RouterLink} from "@angular/router";
import {CommonModule} from "@angular/common";
import {SimpleDatePipe} from "../shared/simple-date/simple-date.pipe";
import {TableSortDirective} from "../shared/table-sort/table-sort.directive";
import {Page} from "../shared/paging/page";
import {Paging} from "../shared/paging/paging";
import {Sort} from "../shared/paging/sort";
import {NgbPagination} from "@ng-bootstrap/ng-bootstrap";

@Component({
  selector: 'app-servers',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    SimpleDatePipe,
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
              private userService: UserService,
              private configService: PropertiesService,
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
    // send to a random area being handled by this server
    // console.log(`server.areaIds: ${server.areaIds}`);
    // let randomServerAreaIdsIndex = Math.floor(Math.random() * server.areaIds.length);
    // console.log(`randomServerAreaIdsIndex: ${randomServerAreaIdsIndex}`);
    // let randomServerAreaId = server.areaIds[randomServerAreaIdsIndex];
    // console.log(`randomServerAreaId: ${randomServerAreaId}`);

    this.userService.navigateCurrentUserOrRegister(server.regionId, server.id).subscribe(user => {
      this.router.navigate(['client'], {
        // queryParams: {
        //   areaId: randomServerAreaId
        // }
      });
    });
  }

  onPageChange(pageIndex: number) {
    this.paging.page = pageIndex;
    this.refreshServers();
  }

  onSort(sort: SortEvent) {
    this.paging.sort = [new Sort(sort.column, sort.direction)];
    this.refreshServers();
  }
}
