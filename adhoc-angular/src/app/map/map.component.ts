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

import {ServerService} from '../server/server.service';
import {Component, DoCheck, Inject, Injector, IterableDiffers, KeyValueDiffers, OnChanges, OnDestroy, OnInit, SimpleChanges} from '@angular/core';
import {Objective} from '../objective/objective';
import {Faction} from '../faction/faction';
import {ObjectiveService} from '../objective/objective.service';
import {FactionService} from '../faction/faction.service';
import {forkJoin} from 'rxjs';
import {Server} from '../server/server';
import {UserService} from "../user/user.service";
import {Region} from "../region/region";
import {Area} from "../area/area";
import {RegionService} from "../region/region.service";
import {AreaService} from "../area/area.service";
import {fabric} from 'fabric';
import {Pawn} from "../pawn/pawn";
import {PawnService} from "../pawn/pawn.service";
import {PropertiesService} from "../properties/properties.service";
import {ActivatedRoute, Router} from "@angular/router";
import {StompService} from "../core/stomp.service";
import {CommonModule, DOCUMENT} from "@angular/common";
import {CsrfService} from "../core/csrf.service";
import {MapComponentExtraInterface} from "./map-component-extra-interface";
import {appExtra} from "../app-extra";
import {Page} from "../core/page";
import {Paging} from "../core/paging";

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [
    CommonModule
  ],
  templateUrl: './map.component.html'
})
export class MapComponent implements OnInit, OnDestroy, DoCheck, OnChanges {
  regions: Region[] = [];
  areas: Area[] = [];
  objectives: Objective[] = [];
  factions: Faction[] = [];
  servers: Server[] = [];
  serversPawns: { [key: number]: Pawn[] } = {};

  objectiveGroups: { [key: number]: fabric.Group } = {};
  pawnGroups: { [key: number]: fabric.Group } = {};

  mapScaleDiffer: any;

  canvasWidth = 1000;
  canvasHeight = 1000;

  canvas: fabric.Canvas = null;

  mapWidth: number = 0;
  mapHeight: number = 0;
  mapLeft: number = 0;
  mapTop: number = 0;

  initialMapScale: number = null;
  mapScale: number = null;

  isDragging: boolean = false;
  lastPosX?: number;
  lastPosY?: number;

  // timerSubscription: Subscription;
  // timer: Observable<number> = timer(0, 10000);

  private mapComponentExtra: MapComponentExtraInterface;

  constructor(private csrfService: CsrfService,
              private stompService: StompService,
              private regionService: RegionService,
              private areaService: AreaService,
              private objectiveService: ObjectiveService,
              private factionService: FactionService,
              private serverService: ServerService,
              private pawnService: PawnService,
              private userService: UserService,
              private stomp: StompService,
              private configService: PropertiesService,
              private iterableDiffers: IterableDiffers,
              private keyValueDiffers: KeyValueDiffers,
              private router: Router,
              private activatedRoute: ActivatedRoute,
              private injector: Injector,
              @Inject(DOCUMENT) private document: Document) {

    if (appExtra) {
      this.mapComponentExtra = new appExtra.MapComponentExtra(this, injector);
    }
  }

  ngOnInit(): void {
    this.createDiffers();

    this.canvas = new fabric.Canvas('map-canvas', {
      preserveObjectStacking: true,
      containerClass: 'map-container',
      // width: this.canvasWidth,
      // height: this.canvasHeight,
      imageSmoothingEnabled: false,
      enableRetinaScaling: true,
      renderOnAddRemove: false
    });

    this.loadData();
    // this.timerSubscription = this.timer.subscribe(() => {
    //   this.refreshData();
    // });

    this.csrfService.getCsrf().subscribe(csrf => {
      this.stompService.setCsrf(csrf);
      this.stompService.connect();
    });

    this.stomp
      .observeEvent('ObjectiveTaken')
      .subscribe((event: any) => this.handleObjectiveTaken(event));

    this.stomp
      .observeEvent('ServerPawns')
      .subscribe((event: any) => this.handleServerPawns(event));

    if (this.mapComponentExtra) {
      this.mapComponentExtra.ngOnInit();
    }
  }

  ngOnDestroy(): void {
    // if (this.timerSubscription) {
    //   this.timerSubscription.unsubscribe();
    // }

    this.stompService.disconnect();

    this.canvas.dispose();
    this.canvas = null;
  }

  ngOnChanges(changes: SimpleChanges): void {
  }

  private loadData() {
    forkJoin([
      this.regionService.getRegions(new Paging(0, Number.MAX_SAFE_INTEGER)),
      this.areaService.getAreas(new Paging(0, Number.MAX_SAFE_INTEGER)),
      this.objectiveService.refreshCachedObjectives(),
      this.serverService.getServers(new Paging(0, Number.MAX_SAFE_INTEGER)),
      this.factionService.refreshCachedFactions(),
      this.pawnService.getPawns(new Paging(0, Number.MAX_SAFE_INTEGER)),
    ]).subscribe(data => {
      // TODO
      let regionsPage: Page<Region>;
      let areasPage: Page<Area>;
      let serversPage: Page<Server>;
      let pawnsPage: Page<Pawn>;

      [regionsPage, areasPage, this.objectives, serversPage, this.factions, pawnsPage] = data;

      this.regions = regionsPage.content;
      this.areas = areasPage.content;
      this.servers = serversPage.content;

      for (let pawn of pawnsPage.content) {
        (this.serversPawns[pawn.serverId] ||= []).push(pawn);
      }

      this.refreshMap();
    });
  }

  private createDiffers() {
    this.mapScaleDiffer = this.keyValueDiffers.find({scale: this.mapScale}).create();
  }

  ngDoCheck() {
    let refresh: boolean = false;

    if (this.mapScaleDiffer.diff({scale: this.mapScale})) {
      refresh = true;
    }

    // TODO
    if (refresh) {
      this.refreshMap();
    }
  }

  refreshMap(): void {
    const visitedObjectives: Objective[] = []; // don't draw links twice (so remember which objectives we have drawn links FROM)

    this.canvas.clear();

    this.canvas.setDimensions({width: this.canvasWidth, height: this.canvasHeight});

    let minLeft = 0;
    let minTop = 0;
    let maxLeft = 0;
    let maxTop = 0;

    for (const area of this.areas) {
      if ((area.x - 0.5 * area.sizeX) < minLeft) {
        minLeft = area.x - 0.5 * area.sizeX;
      }
      if ((-area.y - 0.5 * area.sizeY) < minTop) {
        minTop = -area.y - 0.5 * area.sizeY;
      }
      if ((area.x + 0.5 * area.sizeX) > maxLeft) {
        maxLeft = area.x + 0.5 * area.sizeX;
      }
      if ((-area.y + 0.5 * area.sizeY) > maxTop) {
        maxTop = -area.y + 0.5 * area.sizeY;
      }
    }

    // minLeft -= 50;
    // minTop -= 50;
    // maxLeft += 50;
    // maxTop += 50;

    this.mapWidth = maxLeft - minLeft;
    this.mapHeight = maxTop - minTop;

    if (this.initialMapScale === null && this.areas.length > 0) {
      //console.log("minLeft=" + minLeft + " minTop=" + minTop + " maxLeft=" + maxLeft + " maxTop=" + maxTop);
      //console.log("mapWidth=" + this.mapWidth + " mapHeight=" + this.mapHeight);

      // try to fit whole map into canvas for initial scale
      let scaleWidth = this.canvasWidth / this.mapWidth;
      let scaleHeight = this.canvasHeight / this.mapHeight;
      //console.log("scaleWidth=" + scaleWidth + " scaleHeight=" + scaleHeight);

      this.initialMapScale = Math.min(scaleWidth, scaleHeight) * 0.9;
      this.mapScale = this.initialMapScale;
      //console.log("mapScale=" + this.mapScale);

      this.mapLeft = minLeft - (this.mapWidth * 0.04);
      this.mapTop = minTop - (this.mapHeight * 0.04);
      //console.log("mapLeft=" + this.mapLeft + " mapTop=" + this.mapTop);
    }

    // TODO
    let terrainRect = new fabric.Rect({
      originX: 'left',
      originY: 'top',
      left: minLeft - (this.mapWidth * 0.04),
      top: minTop - (this.mapHeight * 0.04),
      width: this.mapWidth + (this.mapWidth * 0.08),
      height: this.mapHeight + (this.mapHeight * 0.08),
      fill: '#EEEEEE',
      rx: 5 * (1 / this.mapScale),
      ry: 5 * (1 / this.mapScale),
      cornerStyle: "circle",
      hoverCursor: 'default',
      selectable: false,
    });
    this.canvas.add(terrainRect);

    for (const region of this.regions) {
      let regionText = new fabric.IText(`${region.name}`, {
        originX: 'center',
        originY: 'center',
        left: region.x,
        top: -region.y,
        fontFamily: 'sans-serif',
        fontSize: 18 * (1 / this.mapScale),
        fill: '#666666',
        textBackgroundColor: '#EEEEEEAA',
        hoverCursor: 'default',
        editable: false,
        selectable: false,
      });
      this.canvas.add(regionText);
    }

    for (const area of this.areas) {
      let areaBox = new fabric.Rect({
        originX: 'center',
        originY: 'center',
        left: area.x,
        top: -area.y,
        width: area.sizeX,
        height: area.sizeY,
        stroke: '#888888',
        strokeDashArray: [2 * (1 / this.mapScale), 2 * (1 / this.mapScale)],
        strokeWidth: 1 * (1 / this.mapScale),
        fill: 'transparent',
        hoverCursor: 'default',
        selectable: false,
      });
      let areaText = new fabric.IText(`${area.name}`, {
        originX: 'center',
        originY: 'top',
        left: area.x,
        top: -area.y - area.sizeY * 0.5 + 5 * (1 / this.mapScale),
        fontFamily: 'sans-serif',
        fontSize: 16 * (1 / this.mapScale),
        fill: '#444444',
        textBackgroundColor: '#EEEEEEAA',
        hoverCursor: 'default',
        editable: false,
        selectable: false,
      });
      this.canvas.add(areaBox);
      this.canvas.add(areaText);
    }

    for (const objective of this.objectives) {
      for (const linkedObjectiveId of objective.linkedObjectiveIds) {
        const linkedObjective = this.objectives.find(o => o.id == linkedObjectiveId);
        if (visitedObjectives.indexOf(linkedObjective) === -1) {
          let linkLine = new fabric.Line([
            objective.x,
            -objective.y,
            linkedObjective.x, // + 15 * (1 / this.mapScale),
            -linkedObjective.y, // + 15 * (1 / this.mapScale),
          ], {
            stroke: '#888888',
            strokeWidth: 1 * (1 / this.mapScale),
            hoverCursor: 'default',
            selectable: false,
          });
          this.canvas.add(linkLine);
        }
      }
      // TODO
      visitedObjectives.push(objective);
    }

    for (const objective of this.objectives) {
      this.createObjectiveGroup(objective);
    }

    for (const pawns of Object.values(this.serversPawns)) {
      for (const pawn of pawns) {
        this.createPawnGroup(pawn);
      }
    }

    for (const server of this.servers) {
      if (server.webSocketUrl) {
        let label = `Server ${server.id}\n(double click to join)`;
        let serverText = new fabric.IText(label, {
          originX: 'center',
          originY: 'center',
          fontFamily: 'sans-serif',
          fontSize: 14 * (1 / this.mapScale),
          textAlign: 'center',
          fill: '#DDDDDD',
          editable: false,
          selectable: false,
          padding: 5,
        });
        let serverRect = new fabric.Rect({
          originX: 'center',
          originY: 'center',
          width: serverText.get('width') + 20 * (1 / this.mapScale),
          height: serverText.get('height') + 20 * (1 / this.mapScale),
          stroke: '#888888',
          strokeWidth: 2 * (1 / this.mapScale),
          fill: '#444444DD',
          selectable: false,
          hasControls: false,
        });
        let serverGroup = new fabric.Group([serverRect, serverText], {
          originX: 'center',
          originY: 'center',
          left: server.x,
          top: -server.y,
          selectable: true,
          lockMovementX: true,
          lockMovementY: true,
          lockRotation: true,
          hasControls: false,
          hoverCursor: 'pointer',
        });
        serverGroup.on('mousedblclick', () => {
          // TODO: should be to zone with preferred server
          this.router.navigate(['client', 'area', server.areaIds[0]], {
            // queryParams: {
            //   areaId: area.id
            // }
          });
        });
        this.canvas.add(serverGroup);
      }
    }

    this.canvas.on('mouse:wheel', (event) => {
      let delta = event.e.deltaY * this.initialMapScale * -0.00001;
      this.mapScale += delta;
      if (this.mapScale > this.initialMapScale * 2) this.mapScale = this.initialMapScale * 2;
      if (this.mapScale < this.initialMapScale * 0.5) this.mapScale = this.initialMapScale * 0.5;

      // TODO: zoom via center

      event.e.preventDefault();
      event.e.stopPropagation();
    });
    this.canvas.on('mouse:down', (event) => {
      //if (event.e.altKey === true) {
      this.isDragging = true;
      this.canvas.selection = false;
      this.lastPosX = event.e.clientX;
      this.lastPosY = event.e.clientY;
      //}
    });
    this.canvas.on('mouse:move', (event) => {
      if (this.isDragging) {
        const diffX = (event.e.clientX - this.lastPosX);
        const diffY = (event.e.clientY - this.lastPosY);
        //console.log("diffX=" + diffX + " diffY=" + diffY);

        let vpt = this.canvas.viewportTransform;
        vpt[4] += diffX;
        vpt[5] += diffY;
        this.canvas.requestRenderAll();

        this.mapLeft -= diffX * (1 / this.mapScale);
        this.mapTop -= diffY * (1 / this.mapScale);

        this.lastPosX = event.e.clientX;
        this.lastPosY = event.e.clientY;
      }
    });
    this.canvas.on('mouse:up', (event) => {
      this.canvas.setViewportTransform(this.canvas.viewportTransform);
      this.isDragging = false;
      this.canvas.selection = true;
    });

    this.canvas.setZoom(this.mapScale);
    this.canvas.absolutePan(new fabric.Point(this.mapLeft * this.mapScale, this.mapTop * this.mapScale));

    this.canvas.requestRenderAll();
  }

  private createObjectiveGroup(objective: Objective) {
    let objectiveRect = new fabric.Rect({
      originX: 'center',
      originY: 'center',
      width: 20 * (1 / this.mapScale),
      height: 20 * (1 / this.mapScale),
      stroke: '#888888',
      strokeWidth: 1 * (1 / this.mapScale),
      fill: this.getFaction(objective.factionId)?.color || 'lightgray',
      hoverCursor: 'default',
      selectable: false,
    });
    let objectiveText = new fabric.IText(objective.name, {
      originX: 'center',
      originY: 'center',
      top: -20 * (1 / this.mapScale),
      fontFamily: 'sans-serif',
      fontSize: 16 * (1 / this.mapScale),
      fill: '#222222',
      textAlign: 'center',
      textBackgroundColor: '#EEEEEEAA',
      hoverCursor: 'default',
      editable: false,
      selectable: false,
    });
    //[objectiveRect, objectiveText]
    let objectiveGroup = new fabric.Group([objectiveRect], {
      originX: 'center',
      originY: 'center',
      left: objective.x,
      top: -objective.y,
      hoverCursor: 'default',
      selectable: false,
      subTargetCheck: true
    });
    objectiveGroup.add(objectiveText);
    this.canvas.add(objectiveGroup);
    this.objectiveGroups[objective.id] = objectiveGroup;
  }

  private createPawnGroup(pawn: Pawn) {
    let pawnCircle = new fabric.Circle({
      originX: 'center',
      originY: 'center',
      left: 0,
      top: 0,
      radius: 5 * (1 / this.mapScale),
      stroke: 'black',
      strokeWidth: 0.1 * (1 / this.mapScale),
      fill: this.getFaction(pawn.factionId)?.color || 'lightgray',
      opacity: 0,
      hoverCursor: 'default',
      selectable: false,
    });
    let pawnText = new fabric.IText(pawn.name, {
      originX: 'left',
      originY: 'center',
      left: 10 * (1 / this.mapScale),
      top: 0,
      fontFamily: 'sans-serif',
      fontSize: 10 * (1 / this.mapScale),
      fill: '#000000',
      textBackgroundColor: '#EEEEEEAA',
      hoverCursor: 'default',
      editable: false,
      selectable: false,
      visible: !!pawn.human, // bot name only visible on mouseover
    });
    //[pawnCircle, pawnText]
    let pawnGroup = new fabric.Group([pawnCircle], {
      originX: 'center',
      originY: 'center',
      left: pawn.x,
      top: -pawn.y,
      hoverCursor: 'default',
      selectable: false,
      subTargetCheck: true
    });
    pawnCircle.on('mouseover', () => {
      pawnText.set('visible', true);
      this.canvas.requestRenderAll();
    });
    pawnCircle.on('mouseout', () => {
      pawnText.set('visible', !!pawn.human);
      this.canvas.requestRenderAll();
    });
    pawnGroup.add(pawnText);
    this.canvas.add(pawnGroup);
    this.pawnGroups[pawn.id] = pawnGroup;
    pawnCircle.animate({'opacity': 100}, {
      duration: 500,
      easing: fabric.util.ease.easeInExpo,
      onChange: this.canvas.requestRenderAll.bind(this.canvas)
    });
  }

  getFaction(factionId: number): Faction {
    return this.factions.find(faction => faction.id === factionId);
  }

  handleObjectiveTaken(event: any) {
    //console.log(`MapComponent::handleObjectiveTaken: event=${JSON.stringify(event)}`);

    let objectiveId: number = event['objectiveId'];
    let factionId: number = event['factionId'];

    for (const objective of this.objectives) {
      if (objective.id == objectiveId) {
        objective.factionId = factionId;
      }
    }

    //if (!this.canvas) {
    //  return;
    //}

    let objectiveGroup = this.objectiveGroups[objectiveId];
    if (!objectiveGroup) {
      return;
    }

    objectiveGroup.item(0).set({
      fill: this.getFaction(factionId)?.color || 'lightgray',
    });
    this.canvas.requestRenderAll();
  }

  handleServerPawns(event: any) {
    let serverId: number = event['serverId'];
    let pawns: Pawn[] = event['pawns'];

    let oldPawns = this.serversPawns[serverId] || [];
    let oldPawnIds = new Set(oldPawns.map(pawn => pawn.id));
    this.serversPawns[serverId] = pawns;

    //if (!this.canvas || !this.document.hasFocus()) {
    //  return;
    //}

    for (let pawn of pawns) {
      let pawnGroup = this.pawnGroups[pawn.id];
      if (pawnGroup) {
        pawnGroup.animate({'left': pawn.x, 'top': -pawn.y}, {
          duration: 1000,
          easing: fabric.util.ease.easeOutExpo,
          onChange: this.canvas.requestRenderAll.bind(this.canvas),
        });
      } else {
        this.createPawnGroup(pawn);
      }
      oldPawnIds.delete(pawn.id);
    }

    for (const oldPawnId of oldPawnIds) {
      let pawnGroup = this.pawnGroups[oldPawnId];
      if (pawnGroup) {
        pawnGroup.item(0).animate({'opacity': 0}, {
          duration: 500,
          easing: fabric.util.ease.easeOutExpo,
          onChange: this.canvas.requestRenderAll.bind(this.canvas),
          onComplete: () => {
            this.canvas.remove(pawnGroup);
            this.canvas.requestRenderAll();
          }
        });
      }
    }
  }
}
