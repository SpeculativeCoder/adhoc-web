import {ChangeDetectionStrategy, Component} from '@angular/core';

@Component({
  selector: 'app-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [],
  templateUrl: './table.component.html',
})
export class TableComponent {
}
