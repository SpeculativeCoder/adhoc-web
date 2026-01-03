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

import {AfterContentInit, ContentChildren, Directive, EventEmitter, Output, QueryList,} from '@angular/core';
import {HeaderSortComponent} from './header-sort.component';
import {Sort} from "../paging/sort";

@Directive({
  selector: 'table[sort], app-table[sort]'
})
export class TableSortDirective implements AfterContentInit {

  @ContentChildren(HeaderSortComponent, {descendants: true}) headers?: QueryList<HeaderSortComponent>;

  @Output('sort') sortEvent$ = new EventEmitter<Sort>();

  ngAfterContentInit() {
    this.headers?.forEach(header => {
      header.sortEvent$.subscribe((sortEvent: Sort) => {
        this.onSort(sortEvent);
      });
    });
  }

  onSort(sortEvent: Sort) {
    // clear the sort direction on the other headers
    this.headers
        ?.filter(header => header.column !== sortEvent.column)
        .forEach(header => header.direction = undefined);

    this.sortEvent$.emit(sortEvent);
  }
}
