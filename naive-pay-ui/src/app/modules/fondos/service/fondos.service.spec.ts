import { TestBed } from '@angular/core/testing';

import { FondosService } from './fondos.service';

describe('FondosService', () => {
  let service: FondosService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(FondosService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
