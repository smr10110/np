import { TestBed } from '@angular/core/testing';

import { RecompensasService } from './recompensas.service';

describe('RecompensasService', () => {
    let service: RecompensasService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(RecompensasService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
