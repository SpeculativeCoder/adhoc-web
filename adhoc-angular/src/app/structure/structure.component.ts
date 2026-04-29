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

import {ChangeDetectionStrategy, Component, computed, OnInit, signal} from '@angular/core';
import {Structure} from './structure';
import {ActivatedRoute, Router} from '@angular/router';
import {StructureService} from './structure.service';
import {Faction} from '../faction/faction';
import {FactionService} from '../faction/faction.service';
import {forkJoin} from 'rxjs';
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {CurrentUser} from '../user/current/current-user';
import {CurrentUserService} from '../user/current/current-user.service';

@Component({
  selector: 'app-structure',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule
  ],
  templateUrl: './structure.component.html'
})
export class StructureComponent implements OnInit {

  protected structure = signal<Structure>(new Structure());

  protected currentUser = signal<CurrentUser | null>(null);

  protected factions = signal<Faction[] | undefined>(undefined);

  constructor(private route: ActivatedRoute,
              private structureService: StructureService,
              private currentUserService: CurrentUserService,
              private factionService: FactionService,
              private router: Router) {
  }

  ngOnInit() {
    const structureId = +(this.route.snapshot.paramMap.get('id')!);
    this.refreshData(structureId);
  }

  private refreshData(structureId: number) {
    forkJoin([
      this.structureService.getStructure(structureId),
      this.currentUserService.getCurrentUser(),
      this.factionService.getCachedFactions()
    ]).subscribe(data => {
      this.structure.set(data[0]);
      this.currentUser.set(data[1]);
      this.factions.set(data[2]);
    });
  }

  protected faction(factionId: number | undefined) {
    return computed(() =>
        this.factions()?.find(faction => faction.id === factionId));
  }

  protected save() {
    this.structureService.updateStructure(this.structure()).subscribe(structure => {
      this.structure.set(structure);
    });
  }

  protected back() {
    this.router.navigateByUrl('/structures');
  }
}
