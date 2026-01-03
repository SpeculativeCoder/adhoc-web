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

import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, signal} from '@angular/core';
import {MessageService} from './message.service';
import {CommonModule} from "@angular/common";
import {HeaderSortComponent} from "../shared/table-sort/header-sort.component";
import {Router} from "@angular/router";
import {TableSortDirective} from "../shared/table-sort/table-sort.directive";
import {SimpleDatePipe} from "../shared/simple-date/simple-date.pipe";
import {Page} from "../shared/paging/page";
import {Message} from "./message";
import {Paging} from "../shared/paging/paging";
import {forkJoin} from "rxjs";
import {Sort} from "../shared/paging/sort";
import {Pagination} from '../shared/pagination/pagination.component';
import {TableComponent} from '../shared/table/table.component';

@Component({
  selector: 'app-messages',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    SimpleDatePipe,
    TableComponent,
    TableSortDirective,
    HeaderSortComponent,
    Pagination
  ],
  templateUrl: './messages.component.html'
})
export class MessagesComponent implements OnInit {

  messages = signal<Page<Message> | undefined>(undefined);
  private paging: Paging = new Paging();

  constructor(private messageService: MessageService,
              private router: Router,
              private ref: ChangeDetectorRef) {
    this.paging.sort = [new Sort('id', 'desc')];
  }

  ngOnInit() {
    this.refreshMessages();
  }

  private refreshMessages() {
    forkJoin([
      this.messageService.getMessages(this.paging)
    ]).subscribe(data => {
      this.messages.set(data[0]);
    });
  }

  onPageChanged(pageIndex: number) {
    this.paging.page = pageIndex;
    this.refreshMessages();
  }

  onSort(sort: Sort) {
    this.paging.sort = [new Sort(sort.column, sort.direction)];
    this.refreshMessages();
  }
}
