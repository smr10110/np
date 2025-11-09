import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CommerceListComponent } from './commerce-list.component';

describe('CommerceListComponent', () => {
  let component: CommerceListComponent;
  let fixture: ComponentFixture<CommerceListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommerceListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CommerceListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
