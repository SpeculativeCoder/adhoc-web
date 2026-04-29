/*
 * Copyright (c) 2022-2026 SpeculativeCoder (https://github.com/SpeculativeCoder)
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

import {ChangeDetectionStrategy, Component, OnInit, signal} from '@angular/core';
import {ActivatedRoute, Router} from "@angular/router";
import {forkJoin} from "rxjs";
import {Region} from "./region";
import {RegionService} from "./region.service";
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {CurrentUser} from '../user/current/current-user';
import {CurrentUserService} from '../user/current/current-user.service';

@Component({
  selector: 'app-region',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './region.component.html'
})
export class RegionComponent implements OnInit {

  protected region = signal<Region>(new Region());

  protected currentUser = signal<CurrentUser | null>(null);

  constructor(
      private route: ActivatedRoute,
      private regionService: RegionService,
      private currentUserService: CurrentUserService,
      private router: Router) {
  }

  ngOnInit() {
    const regionId = +(this.route.snapshot.paramMap.get('id')!);
    this.refreshData(regionId);
  }

  private refreshData(regionId: number) {
    forkJoin([
      this.regionService.getRegion(regionId),
      this.currentUserService.getCurrentUser()
    ]).subscribe(data => {
      this.region.set(data[0]);
      this.currentUser.set(data[1]);
    });
  }

  protected save() {
    this.regionService.updateRegion(this.region()).subscribe(region => {
      this.region.set(region);
    });
  }

  protected back() {
    this.router.navigateByUrl('/pawns');
  }
}
