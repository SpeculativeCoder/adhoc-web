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
import {Pawn} from './pawn';
import {ActivatedRoute, Router} from '@angular/router';
import {PawnService} from './pawn.service';
import {Faction} from '../faction/faction';
import {FactionService} from '../faction/faction.service';
import {forkJoin} from 'rxjs';
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {SimpleDatePipe} from "../shared/simple-date/simple-date.pipe";

@Component({
  selector: 'app-pawn',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    SimpleDatePipe
  ],
  templateUrl: './pawn.component.html'
})
export class PawnComponent implements OnInit {

  pawn: Pawn = {};
  factions: Faction[] = [];

  constructor(
    private route: ActivatedRoute,
    private pawnService: PawnService,
    private factionService: FactionService,
    private router: Router) {
  }

  ngOnInit() {
    const pawnId = +this.route.snapshot.paramMap.get('id');
    forkJoin([this.pawnService.getPawn(pawnId), this.factionService.getCachedFactions()]).subscribe(data => {
      [this.pawn, this.factions] = data;
    });
  }

  getFaction(factionId: number): Faction {
    return this.factions.find(faction => faction.id === factionId);
  }

  save() {
    this.pawnService.updatePawn(this.pawn).subscribe(pawn => {
      this.pawn = pawn;
    });
  }

  back() {
    this.router.navigateByUrl('/pawns');
  }
}
