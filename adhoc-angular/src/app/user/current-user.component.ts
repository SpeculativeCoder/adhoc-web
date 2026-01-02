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

import {ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit, ViewChild} from '@angular/core';
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {FactionService} from '../faction/faction.service';
import {CurrentUserService} from './current-user.service';
import {MetaService} from '../system/meta.service';
import {Faction} from '../faction/faction';
import {NgbPopover} from '@ng-bootstrap/ng-bootstrap';
import {LogoutService} from './logout/logout.service';
import {Router} from '@angular/router';
import {CurrentUser} from './current-user';

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

  featureFlags: string = '';

  currentUser?: CurrentUser;
  currentUserFaction?: Faction;

  @ViewChild('currentUserPopover')
  currentUserPopover?: NgbPopover;

  copyToClipboardSuccessText?: string;

  constructor(private factionService: FactionService,
              private currentUserService: CurrentUserService,
              private logoutService: LogoutService,
              private metaService: MetaService,
              private router: Router,
              private ref: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.featureFlags = this.metaService.getFeatureFlags();

    this.currentUserService.getCurrentUser$().subscribe(currentUser => {
      // TODO
      if (currentUser) {
        this.currentUser = currentUser;
        this.ref.markForCheck();

        this.factionService.getCachedFaction(currentUser.factionId!).subscribe(faction => {
          this.currentUserFaction = faction
          this.ref.markForCheck();
        });
      }
    });
  }

  logout() {
    this.logoutService.logout().subscribe(
        output => {
          //this.currentUserPopover?.close();
          //this.router.navigate(['']);
          // TODO
          window.location.href = '';
        },
        error => {
          // TODO
          console.log('Failed to logout');
          //this.logoutErrorMessage = 'Failed to logout';
        });
  }

  copyToClipboard() {
    if (this.currentUser && this.currentUser.quickLoginCode) {
      navigator.clipboard.writeText(this.currentUser.quickLoginCode);
      this.copyToClipboardSuccessText = "Copied!";
    }
  }

  close() {
    this.currentUserPopover?.close();
  }
}
