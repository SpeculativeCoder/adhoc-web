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
import {forkJoin} from "rxjs";
import {Region} from "./region";
import {RegionService} from "./region.service";
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";

@Component({
  selector: 'app-region',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './region.component.html'
})
export class RegionComponent implements OnInit {

  region: Region = {};

  constructor(
    private route: ActivatedRoute,
    private regionService: RegionService,
    private router: Router) {
  }

  ngOnInit() {
    const objectiveId = +(this.route.snapshot.paramMap.get('id')!);
    forkJoin([this.regionService.getRegion(objectiveId)]).subscribe(data => {
      [this.region] = data;
    });
  }

  save() {
    this.regionService.updateRegion(this.region).subscribe(region => {
      this.region = region;
    });
  }

  back() {
    this.router.navigateByUrl('/regions');
  }
}
