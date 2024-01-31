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

import {NgModule} from '@angular/core';
import {BrowserModule} from '@angular/platform-browser';
import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {FormsModule} from '@angular/forms';
import {UsersComponent} from './user/users.component';
import {ObjectivesComponent} from './objective/objectives.component';
import {FactionsComponent} from './faction/factions.component';
import {ObjectiveComponent} from './objective/objective.component';
import {UserComponent} from './user/user.component';
import {FactionComponent} from './faction/faction.component';
import {MessagesComponent} from './message/messages.component';
import {NgbModule} from '@ng-bootstrap/ng-bootstrap';
import {HttpClientInMemoryWebApiModule} from 'angular-in-memory-web-api';
import {InMemoryDataService} from './core/in-memory-data/in-memory-data.service';
import {HttpClientModule} from '@angular/common/http';
import {httpInterceptorProviders} from './core/http-interceptor';
import {environment} from '../environments/environment';
import {HeaderSortComponent} from './shared/table-sort/header-sort.component';
import {TableSortDirective} from './shared/table-sort/table-sort.directive';
import {MapComponent} from './map/map.component';
import {ServersComponent} from './server/servers.component';
import {ServerComponent} from './server/server.component';
import {SimpleDatePipe} from './shared/simple-date/simple-date.pipe';
import {LoginOrRegisterComponent} from './user/login-or-register.component';
import {RegionsComponent} from './region/regions.component';
import {RegionComponent} from './region/region.component';
import {AreasComponent} from './area/areas.component';
import {AreaComponent} from './area/area.component';
import {StructuresComponent} from './structure/structures.component';
import {StructureComponent} from './structure/structure.component';
import {PawnsComponent} from "./pawn/pawns.component";
import {PawnComponent} from "./pawn/pawn.component";
import {ClientComponent} from './client/client.component';
import {InfoPageModule} from "./info-page/info-page.module";
import {AppTitleStrategy} from "./app-title-strategy";
import {TitleStrategy} from "@angular/router";

@NgModule({
  declarations: [
    AppComponent,
    MessagesComponent,
    HeaderSortComponent,
    TableSortDirective,
    SimpleDatePipe,
    MapComponent,
    ServersComponent,
    ServerComponent,
    RegionsComponent,
    RegionComponent,
    AreasComponent,
    AreaComponent,
    ObjectivesComponent,
    ObjectiveComponent,
    StructuresComponent,
    StructureComponent,
    PawnsComponent,
    PawnComponent,
    FactionsComponent,
    FactionComponent,
    UsersComponent,
    UserComponent,
    LoginOrRegisterComponent,
    ClientComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    AppRoutingModule,
    HttpClientModule,
    environment.inMemoryDb ? HttpClientInMemoryWebApiModule.forRoot(InMemoryDataService, {dataEncapsulation: false}) : [],
    NgbModule,
    InfoPageModule
  ],
  providers: [
    httpInterceptorProviders,
    {provide: 'BASE_URL', useValue: environment.baseUrl},
    {provide: TitleStrategy, useClass: AppTitleStrategy},
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
