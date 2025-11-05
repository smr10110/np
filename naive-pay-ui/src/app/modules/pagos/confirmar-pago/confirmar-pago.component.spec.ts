import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfirmarPagoComponent } from './confirmar-pago.component';

describe('ConfirmarPagoComponent', () => {
  let component: ConfirmarPagoComponent;
  let fixture: ComponentFixture<ConfirmarPagoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmarPagoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConfirmarPagoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
