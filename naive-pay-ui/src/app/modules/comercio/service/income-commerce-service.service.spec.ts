import { TestBed } from '@angular/core/testing';

import { IncomeCommerceServiceService } from './income-commerce-service.service';

describe('IncomeCommerceServiceService', () => {
  let service: IncomeCommerceServiceService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(IncomeCommerceServiceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
