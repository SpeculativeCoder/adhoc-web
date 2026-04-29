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
import {Area} from "./area";
import {AreaService} from "./area.service";
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {CurrentUserService} from '../user/current/current-user.service';
import {CurrentUser} from '../user/current/current-user';
import {forkJoin} from 'rxjs';

@Component({
  selector: 'app-area',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './area.component.html'
})
export class AreaComponent implements OnInit {

  protected area = signal<Area>(new Area());

  protected currentUser = signal<CurrentUser | null>(null);

  constructor(private route: ActivatedRoute,
              private areaService: AreaService,
              private currentUserService: CurrentUserService,
              private router: Router) {
  }

  ngOnInit() {
    const objectiveId = +this.route.snapshot.paramMap.get('id')!;
    this.refreshData(objectiveId);
  }

  private refreshData(objectiveId: number) {
    forkJoin([
      this.areaService.getArea(objectiveId),
      this.currentUserService.getCurrentUser()
    ]).subscribe(data => {
      this.area.set(data[0]);
      this.currentUser.set(data[1]);
    })
  }

  save() {
    this.areaService.updateArea(this.area()).subscribe(area => {
      this.area.set(area);
    });
  }

  back() {
    this.router.navigateByUrl('/areas');
  }
}
