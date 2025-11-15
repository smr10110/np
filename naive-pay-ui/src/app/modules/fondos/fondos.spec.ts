import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Fondos } from './fondos';

describe('Fondos', () => {
  let component: Fondos;
  let fixture: ComponentFixture<Fondos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Fondos]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Fondos);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
