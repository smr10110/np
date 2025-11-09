import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Autentificacion } from './autentificacion';

describe('Autentificacion', () => {
  let component: Autentificacion;
  let fixture: ComponentFixture<Autentificacion>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Autentificacion]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Autentificacion);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
