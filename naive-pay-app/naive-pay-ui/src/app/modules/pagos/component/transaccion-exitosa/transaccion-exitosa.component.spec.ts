import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TransaccionExitosaComponent } from './transaccion-exitosa.component';

describe('TransaccionExitosaComponent', () => {
  let component: TransaccionExitosaComponent;
  let fixture: ComponentFixture<TransaccionExitosaComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransaccionExitosaComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TransaccionExitosaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
