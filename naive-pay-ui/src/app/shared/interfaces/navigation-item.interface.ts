export interface NavItem {
  name: string;
  icon: string;
  path?: string;
  new?: boolean;
  subItems?: {   name: string; path: string; pro?: boolean; new?: boolean; icon?: string; }[];
}