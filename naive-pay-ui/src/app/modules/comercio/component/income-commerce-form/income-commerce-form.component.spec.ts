import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IncomeCommerceFormComponent } from './income-commerce-form.component';

describe('IncomeCommerceFormComponent', () => {
  let component: IncomeCommerceFormComponent;
  let fixture: ComponentFixture<IncomeCommerceFormComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IncomeCommerceFormComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(IncomeCommerceFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
