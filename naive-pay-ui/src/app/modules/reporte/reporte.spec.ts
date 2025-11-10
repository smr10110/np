import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReporteComponent } from './component/reporte/reporte.component';

describe('Reporte', () => {
  let component: ReporteComponent;
  let fixture: ComponentFixture<ReporteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReporteComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReporteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
