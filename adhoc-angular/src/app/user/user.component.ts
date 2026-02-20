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

import {ChangeDetectionStrategy, Component, computed, OnInit, signal, WritableSignal} from '@angular/core';
import {User} from './user';
import {ActivatedRoute, Router} from '@angular/router';
import {UserService} from './user.service';
import {Faction} from '../faction/faction';
import {FactionService} from '../faction/faction.service';
import {forkJoin} from 'rxjs';
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {SimpleDatePipe} from "../shared/simple-date/simple-date.pipe";
import {CurrentUser} from './current/current-user';
import {CurrentUserService} from './current/current-user.service';
import {NgbDropdownModule} from '@ng-bootstrap/ng-bootstrap';

@Component({
  selector: 'app-user',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    SimpleDatePipe,
    NgbDropdownModule
  ],
  templateUrl: './user.component.html'
})
export class UserComponent implements OnInit {

  protected user = signal<User>(new User());
  protected currentUser = signal<CurrentUser | undefined>(undefined);
  protected factions = signal<Faction[] | undefined>(undefined);

  constructor(private route: ActivatedRoute,
              private userService: UserService,
              private currentUserService: CurrentUserService,
              private factionService: FactionService,
              private router: Router) {
  }

  ngOnInit() {
    const userId = +(this.route.snapshot.paramMap.get('id')!);
    this.refreshData(userId);
  }

  private refreshData(userId: number) {
    forkJoin([
      this.userService.getUser(userId),
      this.currentUserService.getCurrentUser(),
      this.factionService.getCachedFactions()
    ]).subscribe(data => {
      this.user.set(data[0]);
      this.currentUser.set(data[1]);
      this.factions.set(data[2]);
    });
  }

  protected faction(factionId: number | undefined) {
    return computed(() =>
        this.factions()?.find(faction => faction.id === factionId));
  }

  protected setUserFaction(user: WritableSignal<User>, faction: Faction) {
    user.update(user => {
      user.factionId = faction.id;
      return user;
    });
  }

  protected save() {
    this.userService.updateUser(this.user()).subscribe(user => {
      this.user.set(user);
    });
  }

  protected back() {
    this.router.navigateByUrl('/users');
  }
}
