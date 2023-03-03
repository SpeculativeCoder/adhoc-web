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

import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {DomSanitizer, SafeResourceUrl} from "@angular/platform-browser";
import {ConfigService} from "../config/config.service";
import {appEnvironment} from "../../environments/app-environment";

@Component({
  selector: 'app-client',
  templateUrl: './client.component.html',
  styleUrls: ['./client.component.scss']
})
export class ClientComponent implements OnInit {

  showClient: boolean;

  showCompatibilityWarning: boolean;

  mapName: string;

  clientUrl: SafeResourceUrl;

  adhocEnvironment = appEnvironment;

  constructor(private route: ActivatedRoute, private router: Router, private sanitizer: DomSanitizer,
              private configService: ConfigService) {
  }

  ngOnInit() {
    this.mapName = this.route.snapshot.paramMap.get('mapName');
    console.log("mapName: " + this.mapName);

    let commandLine = window.sessionStorage.getItem('UnrealEngine_CommandLine');
    console.log("commandLine: " + commandLine);

    if (!this.mapName || !commandLine || !this.mapName.match("^Region[0-9]+$")) {
      this.router.navigate(['']);
      return;
    }

    // NOTE: mapName has been sanitized above via regex
    this.clientUrl = this.sanitizer.bypassSecurityTrustResourceUrl(
      location.protocol + '//' + location.host + '/' + this.mapName + '/Client.html');

    if (window.navigator.userAgent.indexOf('Windows') != -1) {
      this.showClient = true;
    } else {
      this.showCompatibilityWarning = true;
    }
  }

  runAnyway() {
    this.showCompatibilityWarning = false;
    this.showClient = true;
  }
}
