import { TestBed } from '@angular/core/testing';

import { SearchCommerceServiceService } from './search-commerce-service.service';

describe('SearchCommerceServiceService', () => {
  let service: SearchCommerceServiceService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SearchCommerceServiceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
