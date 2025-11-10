import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Comercio } from './comercio';

describe('Comercio', () => {
  let component: Comercio;
  let fixture: ComponentFixture<Comercio>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Comercio]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Comercio);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
