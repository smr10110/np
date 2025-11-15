import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { PageBreadcrumbComponent } from '../../../../shared/components/common/page-breadcrumb/page-breadcrumb.component';
import { CheckboxComponentsComponent } from '../../../../shared/components/form/form-elements/checkbox-components/checkbox-components.component';
import { DefaultInputsComponent } from '../../../../shared/components/form/form-elements/default-inputs/default-inputs.component';
import { DropzoneComponent } from '../../../../shared/components/form/form-elements/dropzone/dropzone.component';
import { FileInputExampleComponent } from '../../../../shared/components/form/form-elements/file-input-example/file-input-example.component';
import { InputGroupComponent } from '../../../../shared/components/form/form-elements/input-group/input-group.component';
import { InputStatesComponent } from '../../../../shared/components/form/form-elements/input-states/input-states.component';
import { RadioButtonsComponent } from '../../../../shared/components/form/form-elements/radio-buttons/radio-buttons.component';
import { SelectInputsComponent } from '../../../../shared/components/form/form-elements/select-inputs/select-inputs.component';
import { TextAreaInputComponent } from '../../../../shared/components/form/form-elements/text-area-input/text-area-input.component';
import { ToggleSwitchComponent } from '../../../../shared/components/form/form-elements/toggle-switch/toggle-switch.component';

@Component({
  selector: 'app-form-elements',
  imports: [
    CommonModule,
    PageBreadcrumbComponent,
    DefaultInputsComponent,
    SelectInputsComponent,
    TextAreaInputComponent,
    InputStatesComponent,
    InputGroupComponent,
    FileInputExampleComponent,
    CheckboxComponentsComponent,
    RadioButtonsComponent,
    ToggleSwitchComponent,
    DropzoneComponent,
  ],
  templateUrl: './form-elements.component.html',
  styles: ``
})
export default class FormElementsComponent {

}
