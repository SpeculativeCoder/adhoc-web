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

import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {DomSanitizer, SafeResourceUrl} from "@angular/platform-browser";
import {MetaService} from "../system/meta.service";
import {AreaService} from "../area/area.service";
import {UserService} from "../user/user.service";
import {ServerService} from "../server/server.service";
import {customization} from "../customization";
import {CommonModule} from "@angular/common";
import {CurrentUserService} from '../user/current-user.service';

@Component({
  selector: 'app-client',
  standalone: true,
  imports: [
    CommonModule
  ],
  templateUrl: './client.component.html'
})
export class ClientComponent implements OnInit {

  title = customization.title;

  showClient: boolean;
  showCompatibilityWarning: boolean;

  clientUrl: SafeResourceUrl;

  private areaId: number;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private sanitizer: DomSanitizer,
              private metaService: MetaService,
              private areaService: AreaService,
              private serverService: ServerService,
              private userService: UserService,
              private currentUserService: CurrentUserService) {
  }

  ngOnInit() {
    let userAgentCompatible = this.isUserAgentCompatible();

    if (userAgentCompatible) {
      this.runClient();
    } else {
      this.showCompatibilityWarning = true;
    }
  }

  private isUserAgentCompatible(): boolean {
    if (window.navigator.userAgent.indexOf('Windows') != -1) {
      //|| window.navigator.userAgent.indexOf('Intel Mac OS') != -1) {
      return true;
    }
    return false;
  }

  runClientAnyway() {
    this.showCompatibilityWarning = false;

    this.runClient();
  }

  private runClient() {
    // let areaIdString: string = this.route.snapshot.paramMap.get('areaId');
    // console.log(`areaId: ${areaIdString}`);
    // if (!areaIdString) {
    //   throw new Error(`areaId not set or empty`);
    // }
    //
    // this.areaId = Number.parseInt(areaIdString);
    // if (Number.isNaN(this.areaId)) {
    //   throw new Error(`areaId is not a number: ${areaIdString}`);
    // }

    // TODO: allow user and navigation information to be passed into this component?
    this.userService.getCurrentUserOrRegister().subscribe(user => {
      this.currentUserService.navigateCurrentUser().subscribe(navigation => {
        if (!navigation.ip) {
          throw new Error(`Navigation response has no IP`);
        }
        if ((typeof navigation.port) !== 'number') {
          throw new Error(`Navigation response has no port`);
        }
        if (!navigation.webSocketUrl) {
          throw new Error(`Navigation response has no websocket URL`);
        }

        let unrealEngineCommandLine = navigation.ip + ":" + navigation.port;
        if (user && (typeof user.id) === 'number' && (typeof user.factionId) === 'number' && user.token) {
          unrealEngineCommandLine += '?UserID=' + user.id + '?FactionID=' + user.factionId + '?Token=' + user.token;
        }
        // if user is joining region they were last in, try to start their initial location where they may be spawned at
        if ((typeof navigation.x) === 'number' && (typeof navigation.y) === 'number' && (typeof navigation.z) === 'number'
          && (typeof navigation.pitch) === 'number' && (typeof navigation.yaw) === 'number') {
          unrealEngineCommandLine += '?X=' + navigation.x + '?Y=' + navigation.y + '?Z=' + navigation.z + '?Pitch=' + navigation.pitch + '?Yaw=' + navigation.yaw;
        }
        if (this.metaService.featureFlags) {
          unrealEngineCommandLine += ' FeatureFlags=' + this.metaService.featureFlags;
        }
        unrealEngineCommandLine += ' -stdout';
        console.log(`unrealEngineCommandLine: ${unrealEngineCommandLine}`);

        window.sessionStorage.setItem('UnrealEngine_CommandLine', unrealEngineCommandLine);

        if (navigation.webSocketUrl) {
          console.log(`webSocketUrl: ${navigation.webSocketUrl}`);
          window.sessionStorage.setItem('UnrealEngine_WebSocketUrl', navigation.webSocketUrl);
        } else {
          window.sessionStorage.removeItem('UnrealEngine_WebSocketUrl');
        }

        if (!navigation.mapName || !navigation.mapName.match("^[A-Za-z0-9_]{1,50}$")) {
          throw new Error(`invalid map name: ${navigation.mapName}`);
        }

        const clientUrl = location.protocol + '//' + location.host + '/HTML5Client/' + navigation.mapName + '/HTML5Client.html';
        console.log(`clientUrl: ${clientUrl}`);

        //if (!this.isUserAgentCompatible()) {
        //  // can prefer to send to the client directly rather than iframe
        //  window.location.href = clientUrl;
        //  return;
        //}

        // NOTE: mapName as part of clientUrl was sanitized above via regex to be only alphanumeric/underscores
        this.clientUrl = this.sanitizer.bypassSecurityTrustResourceUrl(clientUrl);
        this.showClient = true;
      });
    });
  }
}
