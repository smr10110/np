import { Injectable, signal } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { NavItem } from '../interfaces/navigation-item.interface';

@Injectable({
  providedIn: 'root'
})
export class SidebarService {
  private readonly menuItems = signal<NavItem[]>([]);
  private readonly otherMenuItems = signal<NavItem[]>([]);
  private isExpandedSubject = new BehaviorSubject<boolean>(true);
  private isMobileOpenSubject = new BehaviorSubject<boolean>(false);
  private isHoveredSubject = new BehaviorSubject<boolean>(false);

  isExpanded$ = this.isExpandedSubject.asObservable();
  isMobileOpen$ = this.isMobileOpenSubject.asObservable();
  isHovered$ = this.isHoveredSubject.asObservable();

  setOtherMenuItems(items: NavItem[]) {
    this.otherMenuItems.set(items);
  }

  getOtherMenuItems() {
    return this.otherMenuItems();
  }

  setMenuItems(items: NavItem[]) {
    this.menuItems.set(items);
  }

  getMenuItems() {
    return this.menuItems();
  }

  setExpanded(val: boolean) {
    this.isExpandedSubject.next(val);
  }

  toggleExpanded() {
    this.isExpandedSubject.next(!this.isExpandedSubject.value);
  }

  setMobileOpen(val: boolean) {
    this.isMobileOpenSubject.next(val);
  }

  toggleMobileOpen() {
    this.isMobileOpenSubject.next(!this.isMobileOpenSubject.value);
  }

  setHovered(val: boolean) {
    this.isHoveredSubject.next(val);
  }
}
