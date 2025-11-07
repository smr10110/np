import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CanjePuntosComponent } from './canje-puntos.component';

describe('CanjePuntosComponent', () => {
    let component: CanjePuntosComponent;
    let fixture: ComponentFixture<CanjePuntosComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [CanjePuntosComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(CanjePuntosComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
