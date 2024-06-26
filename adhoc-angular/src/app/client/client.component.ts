/*
 * Copyright (c) 2022-2024 SpeculativeCoder (https://github.com/SpeculativeCoder)
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
import {PropertiesService} from "../properties/properties.service";
import {AreaService} from "../area/area.service";
import {UserService} from "../user/user.service";
import {ServerService} from "../server/server.service";
import {appCustomization} from "../app-customization";
import {CommonModule} from "@angular/common";

@Component({
  selector: 'app-client',
  standalone: true,
  imports: [
    CommonModule
  ],
  templateUrl: './client.component.html'
})
export class ClientComponent implements OnInit {

  appTitle = appCustomization.appTitle;

  showClient: boolean;
  showCompatibilityWarning: boolean;

  clientUrl: SafeResourceUrl;

  private areaId: number;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private sanitizer: DomSanitizer,
              private configService: PropertiesService,
              private areaService: AreaService,
              private serverService: ServerService,
              private userService: UserService) {
  }

  ngOnInit() {
    if (window.navigator.userAgent.indexOf('Windows') == -1
      && window.navigator.userAgent.indexOf('Intel Mac OS') == -1) {
      this.showCompatibilityWarning = true;
      return;
    }

    this.runClient();
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
    //
    // this.areaService.getArea(this.areaId).subscribe(area => {
    //   if (!area.serverId) {
    //     throw new Error(`area ${this.areaId} has no server`);
    //   }
    //
    //   this.serverService.getServer(area.serverId).subscribe(server => {

    this.userService.getCurrentUserOrRegister().subscribe(user => {
      if (!user.serverId) {
        throw new Error(`User ${user.id} has no assigned server`);
      }

      this.serverService.getServer(user.serverId).subscribe(server => {
        // TODO: url or ip
        if (!server.webSocketUrl) {
          throw new Error(`Server ${user.serverId} has no websocket URL`);
        }
        if (!(typeof server.publicWebSocketPort)) {
          throw new Error(`Server ${user.serverId} has no websocket port`);
        }

        let unrealEngineCommandLine = server.publicIp + ":" + server.publicWebSocketPort;
        if (user && (typeof user.id) === 'number' && (typeof user.factionId) === 'number' && user.token) {
          unrealEngineCommandLine += '?UserID=' + user.id + '?FactionID=' + user.factionId + '?Token=' + user.token;
        }
        // if user is joining region they were last in, try to start their initial location where they may be spawned at
        if (user && user.regionId === server.regionId
          && (typeof user.x) === 'number' && (typeof user.y) === 'number' && (typeof user.z) === 'number'
          && (typeof user.pitch) === 'number' && (typeof user.yaw) === 'number') {
          unrealEngineCommandLine += '?X=' + user.x + '?Y=' + user.y + '?Z=' + user.z + '?Pitch=' + user.pitch + '?Yaw=' + user.yaw;
        }
        if (this.configService.featureFlags) {
          unrealEngineCommandLine += ' FeatureFlags=' + this.configService.featureFlags;
        }
        unrealEngineCommandLine += ' -stdout';
        console.log(`unrealEngineCommandLine: ${unrealEngineCommandLine}`);

        window.sessionStorage.setItem('UnrealEngine_CommandLine', unrealEngineCommandLine);

        if (server.webSocketUrl) {
          console.log(`webSocketUrl: ${server.webSocketUrl}`);
          window.sessionStorage.setItem('UnrealEngine_WebSocketUrl', server.webSocketUrl);
        } else {
          window.sessionStorage.removeItem('UnrealEngine_WebSocketUrl');
        }

        if (!server.mapName || !server.mapName.match("^[A-Za-z0-9_]{1,50}$")) {
          throw new Error(`invalid map name: ${server.mapName}`);
        }

        const clientUrl = location.protocol + '//' + location.host + '/HTML5Client/' + server.mapName + '/HTML5Client.html';
        console.log(`clientUrl: ${clientUrl}`);

        // uncomment this if we prefer to send to the client directly rather than iframe
        //window.location.href = clientUrl;
        //return;

        // NOTE: mapName as part of clientUrl was sanitized above via regex to be only alphanumeric/underscores
        this.clientUrl = this.sanitizer.bypassSecurityTrustResourceUrl(clientUrl);
        this.showClient = true;
      });
    });
  }
}
