/*
 * Copyright (c) 2022-2023 SpeculativeCoder (https://github.com/SpeculativeCoder)
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
import {RouterModule, Routes} from '@angular/router';
import {ServersComponent} from './server/servers.component';
import {FactionsComponent} from './faction/factions.component';
import {FactionComponent} from './faction/faction.component';
import {ObjectivesComponent} from './objective/objectives.component';
import {ObjectiveComponent} from './objective/objective.component';
import {UsersComponent} from './user/users.component';
import {UserComponent} from './user/user.component';
import {MapComponent} from './map/map.component';
import {ServerComponent} from './server/server.component';
import {LoginOrRegisterComponent} from './user/login-or-register.component';
import {RegionsComponent} from "./region/regions.component";
import {RegionComponent} from "./region/region.component";
import {AreasComponent} from "./area/areas.component";
import {AreaComponent} from "./area/area.component";
import {StructuresComponent} from "./structure/structures.component";
import {StructureComponent} from "./structure/structure.component";
import {PawnsComponent} from "./pawn/pawns.component";
import {PawnComponent} from "./pawn/pawn.component";
import {PageComponent} from "./web/page/page.component";
import {ClientComponent} from "./client/client.component";

const routes: Routes = [
  {path: '', component: MapComponent},
  {path: 'map', component: MapComponent},
  {path: 'servers', component: ServersComponent},
  {path: 'servers/:id', component: ServerComponent},
  {path: 'regions', component: RegionsComponent},
  {path: 'regions/:id', component: RegionComponent},
  {path: 'areas', component: AreasComponent},
  {path: 'areas/:id', component: AreaComponent},
  {path: 'objectives', component: ObjectivesComponent},
  {path: 'objectives/:id', component: ObjectiveComponent},
  {path: 'structures', component: StructuresComponent},
  {path: 'structures/:id', component: StructureComponent},
  {path: 'pawns', component: PawnsComponent},
  {path: 'pawns/:id', component: PawnComponent},
  {path: 'factions', component: FactionsComponent},
  {path: 'factions/:id', component: FactionComponent},
  {path: 'users', component: UsersComponent},
  {path: 'users/:id', component: UserComponent},
  {path: 'login-or-register', component: LoginOrRegisterComponent},
  {path: 'pages/:page', component: PageComponent},
  {path: 'client/area/:areaId', component: ClientComponent},
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
