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

import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit} from '@angular/core';
import {UserService} from './user.service';
import {User} from './user';
import {Faction} from '../faction/faction';
import {FactionService} from '../faction/faction.service';
import {forkJoin} from 'rxjs';
import {TableHeaderSortableComponent} from '../shared/table/table-header-sortable.component';
import {SimpleDatePipe} from "../shared/simple-date/simple-date.pipe";
import {CommonModule} from "@angular/common";
import {RouterLink} from "@angular/router";
import {TableSortDirective} from "../shared/table/table-sort.directive";
import {Page} from "../shared/page";
import {Paging} from "../shared/paging";
import {Sort} from "../shared/sort";
import {NgbPagination} from "@ng-bootstrap/ng-bootstrap";
import {UserDefeatEventService} from './defeated/user-defeat-event.service';

@Component({
  selector: 'app-users',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    SimpleDatePipe,
    TableSortDirective,
    TableHeaderSortableComponent,
    NgbPagination
  ],
  templateUrl: './users.component.html'
})
export class UsersComponent implements OnInit {

  users?: Page<User>;
  private paging: Paging = new Paging();

  factions: Faction[] = [];
  selectedUsers: User[] = [];

  constructor(private userService: UserService,
              private userDefeatEventService: UserDefeatEventService,
              private factionService: FactionService,
              private ref: ChangeDetectorRef) {
  }

  getFaction(factionId: number) {
    return this.factions.find(faction => faction.id === factionId);
  }

  ngOnInit() {
    this.refreshUsers();

    //this.stompService.connect();
  }

  private refreshUsers() {
    forkJoin([
      this.userService.getUsers(this.paging),
      this.factionService.getCachedFactions()
    ]).subscribe(data => {
      [this.users, this.factions] = data;
      this.ref.markForCheck();
    });
  }

  isUserSelected(user: User): boolean {
    return this.selectedUsers.indexOf(user) !== -1;
  }

  userCheckboxChanged(user: User, selected: boolean) {
    if (selected) {
      this.selectedUsers.push(user);
      if (this.selectedUsers.length > 2) {
        this.selectedUsers.shift();
      }
    } else {
      this.selectedUsers.splice(this.selectedUsers.indexOf(user), 1);
    }
  }

  userDefeatUser() {
    const [user, defeatedUser] = this.selectedUsers;
    this.userDefeatEventService.userDefeat(user, defeatedUser);
  }

  onPageChange(pageIndex: number) {
    this.paging.page = pageIndex;
    this.refreshUsers();
  }

  onSort(sort: Sort) {
    this.paging.sort = [new Sort(sort.column, sort.direction)];
    this.refreshUsers();
  }
}
