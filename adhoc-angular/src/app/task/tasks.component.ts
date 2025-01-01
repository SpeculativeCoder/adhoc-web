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
import {TaskService} from './task.service';
import {HeaderSortComponent} from "../shared/table-sort/header-sort.component";
import {CommonModule} from "@angular/common";
import {RouterLink} from "@angular/router";
import {SimpleDatePipe} from "../shared/simple-date/simple-date.pipe";
import {TableSortDirective} from "../shared/table-sort/table-sort.directive";
import {Page} from "../shared/paging/page";
import {Paging} from "../shared/paging/paging";
import {Sort} from "../shared/paging/sort";
import {Task} from './task';
import {NgbPagination} from "@ng-bootstrap/ng-bootstrap";

@Component({
  selector: 'app-tasks',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    SimpleDatePipe,
    HeaderSortComponent,
    TableSortDirective,
    NgbPagination
  ],
  templateUrl: './tasks.component.html'
})
export class TasksComponent implements OnInit {

  tasks: Page<Task> = new Page();
  private paging: Paging = new Paging();

  constructor(private taskService: TaskService) {
  }

  ngOnInit() {
    this.refreshTasks();
  }

  private refreshTasks() {
    forkJoin([
      this.taskService.getTasks(this.paging)
    ]).subscribe(data => {
      [this.tasks] = data;
    });
  }

  onPageChange(pageIndex: number) {
    this.paging.page = pageIndex;
    this.refreshTasks();
  }

  onSort(sort: Sort) {
    this.paging.sort = [new Sort(sort.column, sort.direction)];
    this.refreshTasks();
  }
}
