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

import {Injectable} from '@angular/core';
import {Meta} from '@angular/platform-browser';
import {customization} from '../customization';

@Injectable({
  providedIn: 'root'
})
export class MetaService {

  private readonly featureFlags: string = '';

  constructor(private meta: Meta) {
    this.meta.addTag({name: 'description', content: customization.description});

    //let featureFlagsMetaElement = document.head.querySelector('meta[name=FEATURE_FLAGS]');
    let featureFlagsMetaElement = this.meta.getTag('name="FEATURE_FLAGS"');
    this.featureFlags = featureFlagsMetaElement['content'] || 'development';

    console.log("featureFlags=" + this.featureFlags);
  }

  getFeatureFlags() {
    return this.featureFlags;
  }
}
