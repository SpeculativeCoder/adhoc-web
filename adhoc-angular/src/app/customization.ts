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

/** You can create a copy of this in src/customization and tailor it to your needs. */
export const customization = {
  title: 'WebApp',
  developer: 'the developer(s) of this web page / application',
  documentationUrl: '',
  description: 'WebApp is a multi-user web application',

  /** Any additional about information (e.g. assets used etc.) can go in here. */
  aboutPageMoreHtml: `
<p>
Uses a <a href="https://github.com/SpeculativeCoder/UnrealEngine-HTML5-ES3">fork of Unreal Engine 4.27 with HTML5 ES3 (WebGL 2) platform plugin</a> which is a modified version of the <a href="https://github.com/UnrealEngineHTML5/Documentation">community-supported plugin for UE 4.24</a>
</p>

<p>
Uses <a href="https://github.com/SpeculativeCoder/AdhocPlugin">Adhoc Unreal Plugin</a> and <a href="https://github.com/SpeculativeCoder/adhoc-web">Adhoc Web</a>
</p>

<br/>
  `,

  /** If adhoc-angular-extra is available this can be set to <tt>extra</tt> */
  extra: null
};
