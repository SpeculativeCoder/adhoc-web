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

import {ChangeDetectionStrategy, Component, OnInit, signal} from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {MetaService} from "./system/meta.service";
import {customization} from "./customization";
import {CommonModule} from "@angular/common";
import {CurrentUserService} from './user/current-user.service';
import {CurrentUserComponent} from './user/current-user.component';
import {CurrentUser} from './user/current-user';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    //environment.inMemoryDb ? HttpClientInMemoryWebApiModule.forRoot(InMemoryDataService, {dataEncapsulation: false}) : [],
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    CurrentUserComponent
  ],
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {

  title = signal(customization.title);
  featureFlags = signal('');
  showExtraFeatures = signal(!!customization.extra);

  currentUser = signal<CurrentUser | undefined>(undefined);

  constructor(private currentUserService: CurrentUserService,
              private metaService: MetaService) {
  }

  ngOnInit() {
    this.featureFlags.set(this.metaService.getFeatureFlags());

    this.currentUserService.getCurrentUser$().subscribe(currentUser => {
      this.currentUser.set(currentUser)
    });
  }

  // @HostListener("window:beforeunload")
  // beforeUnload() {
  //   // disconnect websocket if it is open
  //   this.stompService.disconnect();
  // }

  // ngOnDestroy() {
  // }
}
