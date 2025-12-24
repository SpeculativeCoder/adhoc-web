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
import {FactionService} from '../../faction/faction.service';
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {NgbDropdownModule} from "@ng-bootstrap/ng-bootstrap";
import {LoginService} from './login.service';
import {RegisterService} from '../register/register.service';
import {MetaService} from '../../system/meta.service';

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

  featureFlags: string = '';

  loginName: string = '';
  loginCode: string = '';
  loginNameOrEmail: string = '';
  loginPassword: string = '';
  loginRememberMe: boolean = false;

  quickLoginErrorMessage?: string;
  traditionalLoginErrorMessage?: string;

  @ViewChild('loginNameOrEmailInput')
  loginNameOrEmailInput?: ElementRef;

  constructor(private registerService: RegisterService,
              private loginService: LoginService,
              private factionService: FactionService,
              private metaService: MetaService,
              private router: Router) {
  }

  ngOnInit(): void {
    this.featureFlags = this.metaService.getFeatureFlags();
  }

  ngAfterViewInit(): void {
    this.loginNameOrEmailInput?.nativeElement.focus();
  }

  quickLogin(): void {
    let hyphenIdx = (this.loginCode || '').indexOf('-');
    if (hyphenIdx == -1) {
      this.quickLoginErrorMessage = 'Invalid Login Code';
      return;
    }
    this.loginName = this.loginCode.substring(0, hyphenIdx);

    this.loginService.login(this.loginName, this.loginCode, this.loginRememberMe).subscribe(
        output => {
          this.router.navigate(['']);
          // window.location.href = '/';
        },
        error => {
          this.quickLoginErrorMessage = 'Failed to login';
        });
  }

  traditionalLogin(): void {
    this.loginService.login(this.loginNameOrEmail, this.loginPassword, this.loginRememberMe).subscribe(
        output => {
          this.router.navigate(['']);
          // window.location.href = '/';
        },
        error => {
          this.traditionalLoginErrorMessage = 'Failed to login';
        });
  }

}
