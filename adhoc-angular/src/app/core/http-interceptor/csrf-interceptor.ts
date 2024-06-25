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

import {Injectable} from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {mergeMap, Observable} from 'rxjs';
import {CsrfService} from "../csrf.service";

@Injectable({providedIn: 'root'})
export class CsrfInterceptor implements HttpInterceptor {

  constructor(private csrfService: CsrfService) {
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // GET requests don't need CSRF token to be set
    if (req.method === 'GET') {
      return next.handle(req);
    }

    // for non-GET requests, make user we have got the CSRF token then add it to the request
    return this.csrfService.getCsrf().pipe(mergeMap(csrf => {
      req = req.clone({
        headers: req.headers
          .set(csrf.headerName, csrf.token)
        //.set('Content-Type', 'application/json')
        //.set('Authorization', 'Basic ' + window.btoa('user:password'))
      })
      return next.handle(req);
    }));
  }
}
