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

import {AfterViewInit, Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {UserService} from '../user/user.service';
import {Router} from '@angular/router';
import {Faction} from '../faction/faction';
import {User} from '../user/user';
import {FactionService} from '../faction/faction.service';

@Component({
  selector: 'app-login',
  templateUrl: './login-or-register.component.html'
})
export class LoginOrRegisterComponent implements OnInit, AfterViewInit {

  loginUsernameOrEmail: string = '';
  loginPassword: string = '';

  loginErrorMessage: string;
  registerErrorMessage: string;

  user: User = {};
  factions: Faction[] = [];

  @ViewChild('usernameOrEmail')
  usernameOrEmailInput: ElementRef;

  constructor(private userService: UserService, private factionService: FactionService, private router: Router) {
    this.user.name = null; // 'Anon' + Math.floor(Math.random() * 1000000000);
    this.user.email = null; // this.users.email = `${this.users.name}@localhost`;
    this.user.password = null;
    this.user.factionId = null;
  }

  ngOnInit(): void {
    this.factionService.getFactions().subscribe(factions => {
      this.factions = factions;
      this.user.factionId = 1 + Math.floor(Math.random() * this.factions.length);
    });
  }

  ngAfterViewInit(): void {
    this.usernameOrEmailInput.nativeElement.focus();
  }

  getFaction(factionId: number): Faction {
    return this.factions.find(faction => faction.id === factionId);
  }

  login(): void {
    this.userService.login(this.loginUsernameOrEmail, this.loginPassword).subscribe(
      output => {
        this.router.navigate(['']);
        // window.location.href = '/';
      },
      error => {
        this.loginErrorMessage = 'Failed to login';
      });
  }

  register(): void {
    if (this.user.name === '') {
      this.user.name = null;
    }
    if (this.user.email === '') {
      this.user.email = null;
    }
    if (this.user.password === '') {
      this.user.password = null;
    }
    this.userService.register(this.user).subscribe((user: User) => {
      this.router.navigate(['']);
      //window.location.href = '/';
      //this.router.navigateByUrl(`/users/${users.userId}`)
    }, error => {
      this.registerErrorMessage = 'Failed to register';
    });
  }


}
