import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataTableModule } from 'angular2-datatable';
import { AuditviewComponent } from './auditview.component';
import { DeploymentService } from './deployment.service';
import { AuditviewRoutingModule } from './auditview-routing.module';
import { AuditviewService } from './auditview.service';
import { NewlineFilterPipe } from '../customfilter/newlineFilterPipe';
import { AuditviewFilterPipe } from '../customfilter/auditviewFilterPipe';

@NgModule({
  imports: [CommonModule, FormsModule, DataTableModule, AuditviewRoutingModule],
  declarations: [AuditviewComponent, NewlineFilterPipe, AuditviewFilterPipe],
  providers: [AuditviewService]
})
export class AuditviewModule {
}
