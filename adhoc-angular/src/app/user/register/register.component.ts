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
import {Router} from '@angular/router';
import {Faction} from '../../faction/faction';
import {FactionService} from '../../faction/faction.service';
import {UserRegisterRequest} from "./user-register-request";
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {NgbDropdownModule} from "@ng-bootstrap/ng-bootstrap";
import {RegisterService} from './register.service';
import {MetaService} from '../../system/meta.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NgbDropdownModule
  ],
  templateUrl: './register.component.html'
})
export class RegisterComponent implements OnInit {

  featureFlags: string = '';

  quickRegisterRequest: UserRegisterRequest = {};
  traditionalRegisterRequest: UserRegisterRequest = {};

  quickRegisterErrorMessage?: string;
  traditionalRegisterErrorMessage?: string;

  factions: Faction[] = [];

  constructor(private registerService: RegisterService,
              private factionService: FactionService,
              private metaService: MetaService,
              private router: Router) {

    this.quickRegisterRequest.name = undefined;
    this.quickRegisterRequest.email = undefined;
    this.quickRegisterRequest.password = undefined;
    this.quickRegisterRequest.factionId = undefined;
    this.quickRegisterRequest.rememberMe = false;
    this.quickRegisterRequest.human = true;

    this.traditionalRegisterRequest.name = undefined; // 'Anon' + Math.floor(Math.random() * 1000000000);
    this.traditionalRegisterRequest.email = undefined; // this.users.email = `${this.users.name}@localhost`;
    this.traditionalRegisterRequest.password = undefined;
    this.traditionalRegisterRequest.factionId = undefined;
    this.traditionalRegisterRequest.rememberMe = false;
    this.traditionalRegisterRequest.human = true;
  }

  ngOnInit(): void {
    this.featureFlags = this.metaService.getFeatureFlags();

    this.factionService.getCachedFactions().subscribe(factions => {
      this.factions = factions;
      this.quickRegisterRequest.factionId =
          this.traditionalRegisterRequest.factionId =
              1 + Math.floor(Math.random() * this.factions.length);
    });
  }

  getFaction(factionId: number) {
    return this.factions.find(faction => faction.id === factionId);
  }

  quickRegister(): void {
    if (this.quickRegisterRequest.name === '') {
      this.quickRegisterRequest.name = undefined;
    }
    if (this.quickRegisterRequest.email === '') {
      this.quickRegisterRequest.email = undefined;
    }
    if (this.quickRegisterRequest.password === '') {
      this.quickRegisterRequest.password = undefined;
    }
    this.registerService.register(this.quickRegisterRequest).subscribe(user => {
      this.router.navigate(['']);
      //window.location.href = '/';
      //this.router.navigateByUrl(`/users/${users.userId}`)
    }, error => {
      this.quickRegisterErrorMessage = 'Failed to register';
    });
  }

  traditionalRegister(): void {
    if (this.traditionalRegisterRequest.name === '') {
      this.traditionalRegisterRequest.name = undefined;
    }
    if (this.traditionalRegisterRequest.email === '') {
      this.traditionalRegisterRequest.email = undefined;
    }
    if (this.traditionalRegisterRequest.password === '') {
      this.traditionalRegisterRequest.password = undefined;
    }
    this.registerService.register(this.traditionalRegisterRequest).subscribe(user => {
      this.router.navigate(['']);
      //window.location.href = '/';
      //this.router.navigateByUrl(`/users/${users.userId}`)
    }, error => {
      this.traditionalRegisterErrorMessage = 'Failed to register';
    });
  }
}
