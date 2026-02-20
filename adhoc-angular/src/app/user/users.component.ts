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

import {ChangeDetectionStrategy, Component, computed, OnInit, signal} from '@angular/core';
import {UserService} from './user.service';
import {User} from './user';
import {FactionService} from '../faction/faction.service';
import {TableHeaderSortableComponent} from '../shared/table/table-header-sortable.component';
import {SimpleDatePipe} from "../shared/simple-date/simple-date.pipe";
import {CommonModule} from "@angular/common";
import {RouterLink} from "@angular/router";
import {TableSortDirective} from "../shared/table/table-sort.directive";
import {Paging} from "../shared/paging";
import {Sort} from "../shared/sort";
import {UserDefeatService} from './defeat/user-defeat.service';
import {TableComponent} from '../shared/table/table.component';
import {Page} from '../shared/page';
import {Pagination} from '../shared/pagination/pagination.component';
import {CurrentUserService} from './current/current-user.service';
import {CurrentUser} from './current/current-user';
import {MetaService} from '../system/meta.service';
import {forkJoin} from 'rxjs';
import {Faction} from '../faction/faction';

@Component({
  selector: 'app-users',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterLink,
    SimpleDatePipe,
    TableComponent,
    TableSortDirective,
    TableHeaderSortableComponent,
    Pagination
  ],
  templateUrl: './users.component.html'
})
export class UsersComponent implements OnInit {

  protected featureFlags = signal('');

  protected users = signal<Page<User> | undefined>(undefined);
  protected currentUser = signal<CurrentUser | undefined>(undefined);
  protected factions = signal<Faction[] | undefined>(undefined);

  protected selectedUsers = signal<User[]>([]);

  private paging: Paging = new Paging();

  constructor(private metaService: MetaService,
              private userService: UserService,
              private currentUserService: CurrentUserService,
              private factionService: FactionService,
              private userDefeatService: UserDefeatService) {
  }

  ngOnInit() {
    this.featureFlags.set(this.metaService.getFeatureFlags());

    this.refreshData();
  }

  private refreshData() {
    forkJoin([
      this.userService.getUsers(this.paging),
      this.currentUserService.getCurrentUser(),
      this.factionService.getCachedFactions(),
    ]).subscribe(data => {
      this.users.set(data[0]);
      this.currentUser.set(data[1]);
      this.factions.set(data[2]);
    });
  }

  protected faction(factionId: number | undefined) {
    return computed(() =>
        this.factions()?.find(faction => faction.id === factionId));
  }

  protected setUserSelected(user: User, selected: boolean) {
    if (selected) {
      this.selectedUsers().push(user);
      if (this.selectedUsers().length > 2) {
        this.selectedUsers().shift();
      }
    } else {
      this.selectedUsers().splice(this.selectedUsers().indexOf(user), 1);
    }
  }

  protected serverUserDefeat() {
    const [user, defeatedUser] = this.selectedUsers();
    this.userDefeatService.serverUserDefeat(user, defeatedUser);
  }

  protected onPageChanged(pageIndex: number) {
    this.paging.page = pageIndex;
    this.refreshData();
  }

  protected onSort(sort: Sort) {
    this.paging.sort = [new Sort(sort.column, sort.direction)];
    this.refreshData();
  }
}
