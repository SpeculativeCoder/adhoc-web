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
import {Task} from './task';
import {ActivatedRoute, Router} from '@angular/router';
import {TaskService} from './task.service';
import {forkJoin} from 'rxjs';
import {CommonModule} from "@angular/common";
import {FormsModule} from "@angular/forms";
import {SimpleDatePipe} from "../shared/simple-date/simple-date.pipe";

@Component({
  selector: 'app-task',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    SimpleDatePipe
  ],
  templateUrl: './task.component.html'
})
export class TaskComponent implements OnInit {
  task: Task = {};

  constructor(
    private route: ActivatedRoute,
    private taskService: TaskService,
    private router: Router
  ) {
  }

  ngOnInit() {
    const taskId = +this.route.snapshot.paramMap.get('id');
    forkJoin([this.taskService.getTask(taskId)]).subscribe(data => {
      [this.task] = data;
    });
  }

  save() {
    this.taskService.updateTask(this.task).subscribe(task => {
      this.task = task;
    });
  }

  back() {
    this.router.navigateByUrl('/tasks');
  }
}
