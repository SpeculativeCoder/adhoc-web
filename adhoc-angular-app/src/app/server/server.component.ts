/*
 * Copyright (c) 2022-2024 SpeculativeCoder (https://github.com/SpeculativeCoder)
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
import {Server} from './server';
import {ActivatedRoute, Router} from '@angular/router';
import {ServerService} from './server.service';
import {forkJoin} from 'rxjs';

@Component({
  selector: 'app-servers',
  templateUrl: './server.component.html'
})
export class ServerComponent implements OnInit {
  server: Server = {};

  constructor(
    private route: ActivatedRoute,
    private serverService: ServerService,
    private router: Router
  ) {
  }

  ngOnInit() {
    const serverId = +this.route.snapshot.paramMap.get('id');
    forkJoin([this.serverService.getServer(serverId)]).subscribe(data => {
      [this.server] = data;
    });
  }

  save() {
    this.serverService.updateServer(this.server).subscribe(server => {
      this.server = server;
    });
  }

  back() {
    this.router.navigateByUrl('/servers');
  }
}
