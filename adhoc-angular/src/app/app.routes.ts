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

import {Routes} from '@angular/router';
import {MapComponent} from "./map/map.component";
import {ServersComponent} from "./server/servers.component";
import {ServerComponent} from "./server/server.component";
import {TasksComponent} from "./task/tasks.component";
import {TaskComponent} from "./task/task.component";
import {RegionsComponent} from "./region/regions.component";
import {RegionComponent} from "./region/region.component";
import {AreasComponent} from "./area/areas.component";
import {AreaComponent} from "./area/area.component";
import {ObjectivesComponent} from "./objective/objectives.component";
import {ObjectiveComponent} from "./objective/objective.component";
import {StructuresComponent} from "./structure/structures.component";
import {StructureComponent} from "./structure/structure.component";
import {PawnsComponent} from "./pawn/pawns.component";
import {PawnComponent} from "./pawn/pawn.component";
import {FactionsComponent} from "./faction/factions.component";
import {FactionComponent} from "./faction/faction.component";
import {UsersComponent} from "./user/users.component";
import {UserComponent} from "./user/user.component";
import {LoginOrRegisterComponent} from "./user/login-or-register.component";
import {InfoPageComponent} from "./info-page/info-page.component";
import {ClientComponent} from "./client/client.component";
import {MessagesComponent} from "./message/messages.component";

export const routes: Routes = [
  {path: '', title: '', component: MapComponent},
  {path: 'map', title: 'Map', component: MapComponent},
  {path: 'servers', title: 'Servers', component: ServersComponent},
  {path: 'servers/:id', title: 'Server', component: ServerComponent},
  {path: 'tasks', title: 'Tasks', component: TasksComponent},
  {path: 'tasks/:id', title: 'Task', component: TaskComponent},
  {path: 'regions', title: 'Regions', component: RegionsComponent},
  {path: 'regions/:id', title: 'Region', component: RegionComponent},
  {path: 'areas', title: 'Areas', component: AreasComponent},
  {path: 'areas/:id', title: 'Area', component: AreaComponent},
  {path: 'objectives', title: 'Objectives', component: ObjectivesComponent},
  {path: 'objectives/:id', title: 'Objective', component: ObjectiveComponent},
  {path: 'structures', title: 'Structures', component: StructuresComponent},
  {path: 'structures/:id', title: 'Structure', component: StructureComponent},
  {path: 'pawns', title: 'Pawns', component: PawnsComponent},
  {path: 'pawns/:id', title: 'Pawn', component: PawnComponent},
  {path: 'factions', title: 'Factions', component: FactionsComponent},
  {path: 'factions/:id', title: 'Faction', component: FactionComponent},
  {path: 'users', title: 'Users', component: UsersComponent},
  {path: 'users/:id', title: 'User', component: UserComponent},
  {path: 'messages', title: 'Messages', component: MessagesComponent},
  {path: 'login-or-register', title: 'Login or Register', component: LoginOrRegisterComponent},
  {path: 'pages/:page', title: '', component: InfoPageComponent},
  {path: 'client', title: 'Client', component: ClientComponent},
];
