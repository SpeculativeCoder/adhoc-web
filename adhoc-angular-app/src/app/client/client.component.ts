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
import {appCustomization} from "../../customization/app-customization";
import {AreaService} from "../area/area.service";
import {UserService} from "../user/user.service";
import {ServerService} from "../server/server.service";

@Component({
  selector: 'app-client',
  templateUrl: './client.component.html'
})
export class ClientComponent implements OnInit {

  showClient: boolean;
  showCompatibilityWarning: boolean;

  appEnvironment = appCustomization;

  clientUrl: SafeResourceUrl;

  private areaId: number;

  constructor(private route: ActivatedRoute,
              private router: Router,
              private sanitizer: DomSanitizer,
              private configService: ConfigService,
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
    let areaIdString: string = this.route.snapshot.paramMap.get('areaId');
    console.log(`areaId: ${areaIdString}`);
    if (!areaIdString) {
      throw new Error(`areaId not set or empty`);
    }

    this.areaId = Number.parseInt(areaIdString);
    if (Number.isNaN(this.areaId)) {
      throw new Error(`areaId is not a number: ${areaIdString}`);
    }

    this.areaService.getArea(this.areaId).subscribe(area => {
      if (!area.serverId) {
        throw new Error(`area ${this.areaId} has no server`);
      }

      this.serverService.getServer(area.serverId).subscribe(server => {

        this.userService.getCurrentUserOrRegister(server.id).subscribe(user => {
          let unrealEngineCommandLine = server.publicIp + ":" + server.publicWebSocketPort;
          if (user && (typeof user.id) === 'number' && (typeof user.factionId) === 'number' && user.token) {
            unrealEngineCommandLine += '?UserID=' + user.id + '?FactionID=' + user.factionId + '?Token=' + user.token;
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
    });
  }
}
