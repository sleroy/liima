import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgSelectModule } from '@ng-select/ng-select';
import { PermissionComponent } from './permission.component';
import { PermissionService } from './permission.service';
import { PermissionRoutingModule } from './permission-routing.module';
import { RestrictionEditComponent } from './restriction-edit.component';
import { RestrictionListComponent } from './restriction-list.component';
import { RestrictionAddComponent } from './restriction-add.component';
import {NgbModule} from "@ng-bootstrap/ng-bootstrap";

@NgModule({
  imports: [ CommonModule, FormsModule, NgSelectModule, PermissionRoutingModule, NgbModule],
  declarations: [ PermissionComponent, RestrictionEditComponent, RestrictionListComponent, RestrictionAddComponent ],
  providers: [ PermissionService ]
})
export class PermissionModule {
}
