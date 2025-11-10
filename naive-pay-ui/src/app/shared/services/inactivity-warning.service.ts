import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class InactivityWarningService {
  private showModalSubject = new BehaviorSubject<boolean>(false);
  public showModal$ = this.showModalSubject.asObservable();

  show(): void {
    this.showModalSubject.next(true);
  }

  hide(): void {
    this.showModalSubject.next(false);
  }
}
