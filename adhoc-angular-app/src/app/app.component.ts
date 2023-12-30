/*
 * Copyright (c) 2022-2023 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

import {StompService} from './stomp/stomp.service';
import {Component, ElementRef, HostListener, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {FactionService} from './faction/faction.service';
import {UserService} from './user/user.service';
import {User} from './user/user';
import {ObjectiveService} from "./objective/objective.service";
import {CsrfService} from "./csrf/csrf.service";
import {HeaderInterceptor} from "./http-interceptor/header-interceptor";
import {ConfigService} from "./config/config.service";
import {Faction} from "./faction/faction";
import {appCustomization} from "./app-customization";

@Component({
  selector: 'app-root',
  // TODO
  //standalone: true,
  // TODO
  //imports: [CommonModule, RouterOutlet],
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit, OnDestroy {
  appTitle = 'WebApp';

  featureFlags: string;

  currentUser: User;
  currentUserFaction: Faction;

  constructor(private csrfService: CsrfService,
              private stompService: StompService,
              private factionService: FactionService,
              private controlPointService: ObjectiveService,
              private userService: UserService,
              private headerInterceptor: HeaderInterceptor,
              private elementRef: ElementRef,
              private configService: ConfigService,
              private route: ActivatedRoute,
              private router: Router) {
  }

  ngOnInit() {
    this.featureFlags = this.configService.featureFlags;
    this.appTitle = appCustomization.appTitle;

    this.userService.getCurrentUser$().subscribe(currentUser => {
      this.currentUser = currentUser;

      if (this.currentUser) {
        this.factionService.getFaction(currentUser.factionId).subscribe(faction => {
          this.currentUserFaction = faction
        });
      }
    });

    this.csrfService.getCsrf().subscribe(csrf => {
      this.headerInterceptor.setCsrf(csrf);

      //   this.stompService.setCsrf(csrf);
      //   this.stompService.connect();
    });
  }

  @HostListener("window:beforeunload")
  beforeUnload() {
    // disconnect websocket if it is open
    this.stompService.disconnect();
  }

  ngOnDestroy() {
  }
}
