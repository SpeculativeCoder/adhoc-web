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

import {ChangeDetectionStrategy, Component, Inject, OnDestroy, OnInit, signal} from '@angular/core';
import {RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {MetaService} from "./system/meta.service";
import {customization} from "./customization";
import {CommonModule, DOCUMENT} from "@angular/common";
import {CurrentUserService} from './user/current/current-user.service';
import {CurrentUserComponent} from './user/current/current-user.component';
import {CurrentUser} from './user/current/current-user';
import {StompService} from './system/stomp.service';

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
export class AppComponent implements OnInit, OnDestroy {

  protected title = signal(customization.title);
  protected showExtraFeatures = signal(!!customization.extra);

  protected featureFlags = signal('');

  protected inSafariIFrame = signal(false);

  //protected started = signal(false);

  protected currentUser = signal<CurrentUser | undefined>(undefined);

  constructor(@Inject(DOCUMENT) private document: Document,
              private metaService: MetaService,
              private currentUserService: CurrentUserService,
              private stompService: StompService) {
  }

  ngOnInit() {
    this.featureFlags.set(this.metaService.getFeatureFlags());

    // don't let the app load in a Safari IFrame as the partitioned third party session cookie doesn't seem to work at the moment
    this.inSafariIFrame.set(this.calculateInSafariIFrame());

    if (!this.inSafariIFrame()) {
      //this.start();
      this.doStart();
    }
  }

  private calculateInSafariIFrame() {
    let safari = this.document.defaultView?.navigator.userAgent.indexOf('Safari') != -1;
    let notChrome = !(this.document.defaultView?.navigator.userAgent.indexOf('Chrome') != -1);
    let iFrame;
    try {
      iFrame = window.top != window.self;
    } catch (exception) {
      iFrame = true;
    }
    console.log("safari=" + safari);
    console.log("notChrome=" + notChrome);
    console.log("iFrame=" + iFrame);
    return safari && notChrome && iFrame;
  }

  protected openInNewTab() {
    document.defaultView?.open(document.URL, '_blank');
  }

  // protected start() {
  //   //if (!(this.document.defaultView?.navigator.userAgent.indexOf('Windows') != -1)) {
  //   console.log("hasStorageAccess");
  //   this.document.hasStorageAccess().then(storageAccess => {
  //     console.log("hasStorageAccess=" + storageAccess);
  //   });
  //   console.log("requestStorageAccess");
  //   this.document.requestStorageAccess().then(() => {
  //     console.log("requestStorageAccess complete");
  //     this.doStart();
  //   }, reason => {
  //     console.log("requestStorageAccess rejected: reason=" + reason);
  //     this.doStart();
  //   });
  //   //}
  // }

  private doStart() {
    this.refreshData();

    this.stompService.connect();

    //this.started.set(true);
  }

  private refreshData() {
    this.currentUserService.getCurrentUser$().subscribe(currentUser => {
      this.currentUser.set(currentUser);
    });
  }

  ngOnDestroy(): void {
    this.stompService.disconnect();
  }

  // @HostListener("window:beforeunload")
  // beforeUnload() {
  //   // disconnect websocket if it is open
  //   this.stompService.disconnect();
  // }
}
