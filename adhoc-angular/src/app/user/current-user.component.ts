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

import {ChangeDetectionStrategy, Component, OnInit, signal, viewChild} from '@angular/core';
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {CurrentUserService} from './current-user.service';
import {MetaService} from '../system/meta.service';
import {Faction} from '../faction/faction';
import {NgbPopover} from '@ng-bootstrap/ng-bootstrap';
import {LogoutService} from './logout/logout.service';
import {Router} from '@angular/router';
import {Observable, of} from 'rxjs';
import {CurrentUser} from './current-user';
import {FactionService} from '../faction/faction.service';

@Component({
  selector: 'app-current-user',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    NgbPopover
  ],
  templateUrl: './current-user.component.html'
})
export class CurrentUserComponent implements OnInit {

  featureFlags = signal<string>('');

  currentUser = signal<CurrentUser | undefined>(undefined);
  currentUserFaction = signal<Faction | undefined>(undefined);

  copyToClipboardSuccessText = signal('');

  currentUserPopover = viewChild.required<NgbPopover>('currentUserPopover');

  constructor(private currentUserService: CurrentUserService,
              private factionService: FactionService,
              private logoutService: LogoutService,
              private metaService: MetaService,
              private router: Router) {
  }

  ngOnInit(): void {
    this.featureFlags.set(this.metaService.getFeatureFlags());

    this.currentUserService.getCurrentUser$().subscribe(currentUser => {

      let faction$: Observable<Faction | undefined> =
          (currentUser && currentUser.factionId)
              ? this.factionService.getCachedFaction(currentUser.factionId!)
              : of(undefined);
      faction$.subscribe(faction => {

        this.currentUser.set(currentUser);
        this.currentUserFaction.set(faction);
      });
    });
  }

  popoverButtonClicked() {
    // if clicking on button to open the popover - then we need to refresh user information as the popover shows
    if (!this.currentUserPopover().isOpen()) {
      this.currentUserService.refreshCurrentUser().subscribe();
    }
  }

  logout() {
    this.logoutService.logout().subscribe({
      next: () => {
        this.currentUserPopover().close();
        this.router.navigate([''])
        //window.location.href = '';
      },
      error: () => {
        // TODO
        console.log('Failed to logout');
        //this.logoutErrorMessage = 'Failed to logout';
      }
    });
  }

  copyToClipboard() {
    if (this.currentUser() && this.currentUser()?.quickLoginCode) {
      // TODO
      navigator.clipboard.writeText(this.currentUser()?.quickLoginCode!);
      this.copyToClipboardSuccessText.set('Copied!');
    }
  }

  close() {
    this.currentUserPopover().close();
  }

}
