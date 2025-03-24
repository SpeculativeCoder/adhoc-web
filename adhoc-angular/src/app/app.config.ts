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

import {ApplicationConfig, provideZoneChangeDetection} from '@angular/core';
import {provideRouter, TitleStrategy, withComponentInputBinding} from '@angular/router';

import {routes} from './app.routes';
import {environment} from "../environments/environment";
import {AppTitleStrategy} from "./app-title-strategy";
import {HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi} from "@angular/common/http";
import {CsrfInterceptor} from "./system/http-interceptor/csrf-interceptor";
import {ErrorInterceptor} from "./system/http-interceptor/error-interceptor";

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({eventCoalescing: true}),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptorsFromDi()), // TODO
    {provide: HTTP_INTERCEPTORS, useExisting: CsrfInterceptor, multi: true},
    {provide: HTTP_INTERCEPTORS, useExisting: ErrorInterceptor, multi: true},
    {provide: 'BASE_URL', useValue: environment.baseUrl},
    {provide: TitleStrategy, useClass: AppTitleStrategy}
  ]
};
