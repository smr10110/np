import { Component } from '@angular/core';
import { ComponentCardComponent } from '../../../../shared/components/common/component-card/component-card.component';
import { PageBreadcrumbComponent } from '../../../../shared/components/common/page-breadcrumb/page-breadcrumb.component';
import { FourIstoThreeComponent } from '../../../../shared/components/ui/videos/four-isto-three/four-isto-three.component';
import { OneIstoOneComponent } from '../../../../shared/components/ui/videos/one-isto-one/one-isto-one.component';
import { SixteenIstoNineComponent } from '../../../../shared/components/ui/videos/sixteen-isto-nine/sixteen-isto-nine.component';
import { TwentyoneIstoNineComponent } from '../../../../shared/components/ui/videos/twentyone-isto-nine/twentyone-isto-nine.component';

@Component({
  selector: 'app-videos',
  imports: [
    PageBreadcrumbComponent,
    ComponentCardComponent,
    SixteenIstoNineComponent,
    FourIstoThreeComponent,
    OneIstoOneComponent,
    TwentyoneIstoNineComponent
],
  templateUrl: './videos.component.html',
  styles: ``
})
export default class VideosComponent {

}
