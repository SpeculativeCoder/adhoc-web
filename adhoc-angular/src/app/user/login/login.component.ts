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

import {AfterViewInit, Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {Router} from '@angular/router';
import {Faction} from '../../faction/faction';
import {User} from '../user';
import {FactionService} from '../../faction/faction.service';
import {UserRegisterRequest} from "../register/user-register-request";
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {NgbDropdownModule} from "@ng-bootstrap/ng-bootstrap";
import {LoginService} from './login.service';
import {RegisterService} from '../register/register.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NgbDropdownModule
  ],
  templateUrl: './login.component.html'
})
export class LoginComponent implements OnInit, AfterViewInit {

  loginNameOrEmail: string = '';
  loginPassword: string = '';
  loginRememberMe: boolean = false;

  loginErrorMessage?: string;
  registerErrorMessage?: string;

  userRegisterRequest: UserRegisterRequest = {};

  factions: Faction[] = [];

  @ViewChild('loginNameOrEmailInput')
  loginNameOrEmailInput?: ElementRef;

  constructor(private registerService: RegisterService,
              private loginService: LoginService,
              private factionService: FactionService,
              private router: Router) {
    this.userRegisterRequest.name = undefined; // 'Anon' + Math.floor(Math.random() * 1000000000);
    this.userRegisterRequest.email = undefined; // this.users.email = `${this.users.name}@localhost`;
    this.userRegisterRequest.password = undefined;
    this.userRegisterRequest.factionId = undefined;
    this.userRegisterRequest.rememberMe = false;
    this.userRegisterRequest.human = true;
  }

  ngOnInit(): void {
    this.factionService.getCachedFactions().subscribe(factions => {
      this.factions = factions;
      this.userRegisterRequest.factionId = 1 + Math.floor(Math.random() * this.factions.length);
    });
  }

  ngAfterViewInit(): void {
    this.loginNameOrEmailInput?.nativeElement.focus();
  }

  getFaction(factionId: number) {
    return this.factions.find(faction => faction.id === factionId);
  }

  login(): void {
    this.loginService.login(this.loginNameOrEmail, this.loginPassword, this.loginRememberMe).subscribe(
      output => {
        this.router.navigate(['']);
        // window.location.href = '/';
      },
      error => {
        this.loginErrorMessage = 'Failed to login';
      });
  }

  register(): void {
    if (this.userRegisterRequest.name === '') {
      this.userRegisterRequest.name = undefined;
    }
    if (this.userRegisterRequest.email === '') {
      this.userRegisterRequest.email = undefined;
    }
    if (this.userRegisterRequest.password === '') {
      this.userRegisterRequest.password = undefined;
    }
    this.registerService.register(this.userRegisterRequest).subscribe(user => {
      this.router.navigate(['']);
      //window.location.href = '/';
      //this.router.navigateByUrl(`/users/${users.userId}`)
    }, error => {
      this.registerErrorMessage = 'Failed to register';
    });
  }
}
