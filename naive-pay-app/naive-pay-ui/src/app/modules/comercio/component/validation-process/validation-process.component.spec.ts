import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ValidationProcessComponent } from './validation-process.component';

describe('ValidationProcessComponent', () => {
  let component: ValidationProcessComponent;
  let fixture: ComponentFixture<ValidationProcessComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ValidationProcessComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ValidationProcessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
