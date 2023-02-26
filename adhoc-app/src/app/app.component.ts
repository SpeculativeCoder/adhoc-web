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
import {Component, ElementRef, OnInit} from '@angular/core';
import {FactionService} from './faction/faction.service';
import {UserService} from './user/user.service';
import {User} from './user/user';
import {ObjectiveService} from "./objective/objective.service";
import {CsrfService} from "./csrf/csrf.service";
import {HeaderInterceptor} from "./http-interceptor/header-interceptor";
import {ConfigService} from "./config/config.service";
import {Faction} from "./faction/faction";
import {appEnvironment} from "../environments/app-environment";

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  adhocAppTitle = 'WebApp';

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
              private configService: ConfigService) {
  }

  ngOnInit() {
    let featureFlagsMetaElement = document.head.querySelector('meta[name=FEATURE_FLAGS]');
    this.configService.featureFlags = featureFlagsMetaElement['content'] || 'development';
    console.log("featureFlags=" + this.configService.featureFlags);

    // let adhocAppTitleMetaElement = document.head.querySelector('meta[name=ADHOC_APP_TITLE]');
    // this.configService.adhocAppTitle = adhocAppTitleMetaElement['content'] || 'WebApp';
    // console.log("adhocAppTitle=" + this.configService.adhocAppTitle);
    //
    // let adhocAppDeveloperMetaElement = document.head.querySelector('meta[name=ADHOC_APP_DEVELOPER]');
    // this.configService.adhocAppDeveloper = adhocAppDeveloperMetaElement['content'] || 'the developer(s) of this web page / application';
    // console.log("adhocAppDeveloper=" + this.configService.adhocAppDeveloper);

    this.featureFlags = this.configService.featureFlags;
    this.adhocAppTitle = appEnvironment.appTitle;

    this.userService.watchCurrentUser().subscribe(currentUser => {
      this.currentUser = currentUser;

      if (this.currentUser) {
        this.factionService.getFaction(currentUser.factionId).subscribe(faction => this.currentUserFaction = faction);
      }
    });

    this.csrfService.getCsrf().subscribe(csrf => {
      this.headerInterceptor.setCsrf(csrf);
      this.stompService.setCsrf(csrf);

      this.stompService.connect();
    })

    // let serverDomainMetaElement = document.head.querySelector('meta[name=SERVER_DOMAIN]');
    // this.configService.serverDomain = serverDomainMetaElement['content'] || 'localhost';
    // console.log("serverDomain="+this.configService.serverDomain);
  }

}
