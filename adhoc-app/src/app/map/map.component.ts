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

import {ServerService} from '../server/server.service';
import {Component, DoCheck, IterableDiffers, KeyValueDiffers, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {Objective} from '../objective/objective';
import {Faction} from '../faction/faction';
import {ObjectiveService} from '../objective/objective.service';
import {FactionService} from '../faction/faction.service';
import {forkJoin, Observable, Subscription, timer} from 'rxjs';
import {Server} from '../server/server';
import {User} from "../user/user";
import {UserService} from "../user/user.service";
import {Region} from "../region/region";
import {Area} from "../area/area";
import {RegionService} from "../region/region.service";
import {AreaService} from "../area/area.service";
import {fabric} from 'fabric';
import {Pawn} from "../pawn/pawn";
import {PawnService} from "../pawn/pawn.service";
import {ConfigService} from "../config/config.service";
import {Router} from "@angular/router";

@Component({
  selector: 'app-map',
  templateUrl: './map.component.html'
})
export class MapComponent implements OnInit, DoCheck, OnChanges {
  regions: Region[] = [];
  areas: Area[] = [];
  objectives: Objective[] = [];
  factions: Faction[] = [];
  servers: Server[] = [];
  pawns: Pawn[] = [];

  differ: any;
  regionsDiffer: any;
  regionDiffers: any[] = [];
  areasDiffer: any;
  areaDiffers: any[] = [];
  objectivesDiffer: any;
  objectiveDiffers: any[] = [];
  serversDiffer: any;
  serverDiffers: any[] = [];
  pawnsDiffer: any;
  pawnDiffers: any[] = [];

  canvasWidth = 1000;
  canvasHeight = 1000;

  canvas: fabric.Canvas;

  mapWidth: number = 0;
  mapHeight: number = 0;
  mapLeft: number = 0;
  mapTop: number = 0;
  initialMapScale: number = null;
  mapScale: number = null;

  isDragging: boolean = false;
  lastPosX?: number;
  lastPosY?: number;

  timerSubscription: Subscription;
  timer: Observable<number> = timer(0, 10000);

  constructor(private regionService: RegionService,
              private areaService: AreaService,
              private objectiveService: ObjectiveService,
              private factionService: FactionService,
              private serverService: ServerService,
              private pawnService: PawnService,
              private userService: UserService,
              private featureFlagsService: ConfigService,
              private iterableDiffers: IterableDiffers,
              private keyValueDiffers: KeyValueDiffers,
              private router: Router) {
  }

  ngOnInit(): void {
    this.createDiffers();

    this.canvas = new fabric.Canvas('map-canvas', {
      preserveObjectStacking: true,
      containerClass: 'map-container',
      // width: this.canvasWidth,
      // height: this.canvasHeight,
      imageSmoothingEnabled: false,
      enableRetinaScaling: true
    });

    this.refreshData();
    this.timerSubscription = this.timer.subscribe(() => {
      this.refreshData();
    });
  }

  ngOnDestroy(): void {
    if (this.timerSubscription) {
      this.timerSubscription.unsubscribe();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
  }

  private refreshData() {
    forkJoin([
      this.regionService.getRegions(),
      this.areaService.getAreas(),
      this.objectiveService.getObjectives(),
      this.serverService.getServers(),
      this.factionService.getFactions(),
      this.pawnService.getPawns(),
    ]).subscribe(data => {
      [this.regions, this.areas, this.objectives, this.servers, this.factions, this.pawns] = data;
    });
  }

  private createDiffers() {
    this.differ = this.keyValueDiffers.find({scale: this.mapScale}).create();

    this.regionsDiffer = this.iterableDiffers.find(this.regions).create();
    this.regionDiffers.length = 0;
    this.regions.forEach((region) => {
      this.regionDiffers.push(this.keyValueDiffers.find(region).create());
    });
    this.areasDiffer = this.iterableDiffers.find(this.areas).create();
    this.areaDiffers.length = 0;
    this.areas.forEach((area) => {
      this.areaDiffers.push(this.keyValueDiffers.find(area).create());
    });
    this.objectivesDiffer = this.iterableDiffers.find(this.objectives).create();
    this.objectiveDiffers.length = 0;
    this.objectives.forEach((objective) => {
      this.objectiveDiffers.push(this.keyValueDiffers.find(objective).create());
    });
    this.serversDiffer = this.iterableDiffers.find(this.servers).create();
    this.serverDiffers.length = 0;
    this.servers.forEach((server) => {
      this.serverDiffers.push(this.keyValueDiffers.find(server).create());
    });
    this.pawnsDiffer = this.iterableDiffers.find(this.pawns).create();
    this.pawnDiffers.length = 0;
    this.pawns.forEach((pawn) => {
      this.pawnDiffers.push(this.keyValueDiffers.find(pawn).create());
    });
  }

  ngDoCheck() {
    let refresh: boolean = false;

    if (this.differ.diff({scale: this.mapScale})) {
      refresh = true;
    }

    if (this.regionsDiffer && this.regionsDiffer.diff(this.regions)) {
      refresh = true;
    }
    for (let i = 0; i < this.regionDiffers.length; i++) {
      if (this.regionDiffers[i].diff(this.regions[i])) {
        refresh = true;
      }
    }
    if (this.areasDiffer && this.areasDiffer.diff(this.areas)) {
      refresh = true;
    }
    for (let i = 0; i < this.areaDiffers.length; i++) {
      if (this.areaDiffers[i].diff(this.areas[i])) {
        refresh = true;
      }
    }
    if (this.objectivesDiffer && this.objectivesDiffer.diff(this.objectives)) {
      refresh = true;
    }
    for (let i = 0; i < this.objectiveDiffers.length; i++) {
      if (this.objectiveDiffers[i].diff(this.objectives[i])) {
        refresh = true;
      }
    }
    if (this.serversDiffer && this.serversDiffer.diff(this.servers)) {
      refresh = true;
    }
    for (let i = 0; i < this.serverDiffers.length; i++) {
      if (this.serverDiffers[i].diff(this.servers[i])) {
        refresh = true;
      }
    }
    if (this.pawnsDiffer && this.pawnsDiffer.diff(this.pawns)) {
      refresh = true;
    }
    for (let i = 0; i < this.pawnDiffers.length; i++) {
      if (this.pawnDiffers[i].diff(this.pawns[i])) {
        refresh = true;
      }
    }

    if (refresh) {
      //console.log("refreshing");
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
      height: this.mapHeight+ (this.mapHeight * 0.08),
      fill: '#EEEEEE',
      rx: 5 * (1 / this.mapScale),
      ry: 5 * (1 / this.mapScale),
      cornerStyle: "circle",
      hoverCursor: 'default',
      selectable: false,
    });
    this.canvas.add(terrainRect);

    for (const region of this.regions) {
      let regionText = new fabric.IText("Region " + region.name, {
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
        strokeWidth: 1 * (1 / this.mapScale),
        fill: 'transparent',
        hoverCursor: 'default',
        selectable: false,
      });
      let areaText = new fabric.IText("Area " + area.name, {
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
          let linkLine = new fabric.Line([objective.x, -objective.y, linkedObjective.x, -linkedObjective.y], {
            stroke: '#888888',
            strokeWidth: 1 * (1 / this.mapScale),
            hoverCursor: 'default',
            selectable: false,
          });
          this.canvas.add(linkLine);
        }
      }
    }

    for (const objective of this.objectives) {
      let objectiveRect = new fabric.Rect({
        originX: 'center',
        originY: 'center',
        left: objective.x,
        top: -objective.y,
        width: 20 * (1 / this.mapScale),
        height: 20 * (1 / this.mapScale),
        stroke: '#888888',
        strokeWidth: 1 * (1 / this.mapScale),
        fill: this.getFaction(objective.factionId)?.color || 'lightgray',
        hoverCursor: 'default',
        selectable: false,
      });
      let objectiveText = new fabric.IText(objective.name, {
        fontFamily: 'sans-serif',
        fontSize: 16 * (1 / this.mapScale),
        fill: '#222222',
        textBackgroundColor: '#EEEEEEAA',
        hoverCursor: 'default',
        editable: false,
        selectable: false,
      });
      objectiveText.set({
        left: objective.x - 0.5 * objectiveText.get('width'),
        top: -objective.y - 30 * (1 / this.mapScale),
      });
      this.canvas.add(objectiveRect);
      this.canvas.add(objectiveText);
    }

    for (const pawn of this.pawns) {
      let pawnCircle = new fabric.Circle({
        originX: 'center',
        originY: 'center',
        left: pawn.x,
        top: -pawn.y,
        radius: 5 * (1 / this.mapScale),
        stroke: 'black',
        strokeWidth: 0.1 * (1 / this.mapScale),
        fill: this.getFaction(pawn.factionId)?.color || 'lightgray',
        hoverCursor: 'default',
        selectable: false,
      });
      let label = pawn.name;
      let pawnText = new fabric.IText(label, {
        originX: 'left',
        originY: 'center',
        left: pawn.x + 10 * (1 / this.mapScale),
        top: -pawn.y, // + 10 * (1 / this.scale),
        fontFamily: 'sans-serif',
        fontSize: 10 * (1 / this.mapScale),
        fill: '#000000',
        textBackgroundColor: '#EEEEEEAA',
        hoverCursor: 'default',
        editable: false,
        selectable: false,
        visible: !!pawn.userId, // bot name only visible on mouseover
      });
      pawnCircle.on('mouseover', () => {
        pawnText.set('visible', true);
        this.canvas.requestRenderAll();
      });
      pawnCircle.on('mouseout', () => {
        pawnText.set('visible', !!pawn.userId);
        this.canvas.requestRenderAll();
      });
      this.canvas.add(pawnCircle);
      this.canvas.add(pawnText);
    }

    for (const area of this.areas) {
      for (const server of this.servers) {
        if (server.publicIp && server.areaIds.indexOf(area.id) != -1) {
          let label = `Area ${area.name} - Server ${server.name}\n(double click to join)`;
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
            this.router.navigate(['client', 'area', area.id], {
              // queryParams: {
              //   areaId: area.id
              // }
            });
          });
          this.canvas.add(serverGroup);
        }
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

  getFaction(factionId: number): Faction {
    return this.factions.find(faction => faction.id === factionId);
  }
}
