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

import {Component, EventEmitter, HostListener, Input, Output,} from '@angular/core';
import {CommonModule} from "@angular/common";
import {SortDirection} from "../paging/sort-direction";
import {Sort} from "../paging/sort";

@Component({
  selector: 'th[sortable]',
  standalone: true,
  imports: [
    CommonModule
  ],
  template:
    '<ng-content></ng-content><span class="position-fixed" *ngIf="direction">&nbsp;<span class="small oi {{openIconicIconClass}}"></span></span>'
})
export class HeaderSortComponent {

  // tslint:disable-next-line: no-input-rename
  @Input('sortable') column?: string;

  @Output('sort') sortEvent$ = new EventEmitter<Sort>();

  direction?: SortDirection;

  get openIconicIconClass() {
    return this.direction === 'asc' ? 'oi-sort-ascending' : 'oi-sort-descending';
  }

  @HostListener('click', ['$event'])
  click(event: MouseEvent) {
    this.direction = this.direction === 'asc' ? 'desc' : 'asc';
    this.sortEvent$.emit({column: this.column!, direction: this.direction});
  }
}
