import { NavItem } from "../shared/interfaces/navigation-item.interface";

const HOME_ICON = `
<svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M3 10.5 12 4l9 6.5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M5 10v9a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
</svg>`;

const REPORTS_ICON = `
<svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
  <path d="M7 17V13M12 17V9M17 17V7" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
</svg>`;

const DEVICES_ICON = `
<svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect x="3" y="5" width="13" height="12" rx="2" stroke="currentColor" stroke-width="2"/>
  <path d="M18 9h2a1 1 0 0 1 1 1v7a2 2 0 0 1-2 2h-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <circle cx="9.5" cy="14" r="1" fill="currentColor"/>
</svg>`;

const FUNDS_ICON = `
<svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect x="3" y="5" width="18" height="14" rx="2" stroke="currentColor" stroke-width="2"/>
  <path d="M16 12.5h3.5a1.5 1.5 0 0 0 0-3H16v3Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
  <circle cx="16.5" cy="11" r="1" fill="currentColor"/>
</svg>`;

const PAYMENTS_ICON = `
<svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect x="2" y="5" width="20" height="14" rx="2" stroke="currentColor" stroke-width="2"/>
  <path d="M2 10h20" stroke="currentColor" stroke-width="2"/>
  <circle cx="8" cy="15" r="1.25" fill="currentColor"/>
  <rect x="12" y="13.5" width="6" height="3" rx="1" fill="currentColor"/>
</svg>`;

const REWARDS_ICON = `
<svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M12 3l2.2 4.46 4.92.72-3.56 3.47.84 4.9L12 14.77 7.6 16.55l.84-4.9L4.88 8.18l4.92-.72L12 3Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
</svg>`;

const COMMERCE_ICON = `
<svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M3 9h18v12H3V9Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
  <path d="M4 9l8-6 8 6" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
  <path d="M7 21v-7h10v7" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
</svg>
`;

const AUTH_ICON = `
<svg width="1em" height="1em" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
  <path d="M12 3l7 4v6c0 4.418-3.582 8-8 8s-8-3.582-8-8V7l9-4Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
  <path d="M9.5 12a2.5 2.5 0 0 1 5 0v2.5h-5V12Z" stroke="currentColor" stroke-width="2"/>
</svg>
`;

export const navItems: NavItem[] = [
    { name: 'Home',        path: '/',         icon: HOME_ICON },
    { name: 'Reports',     path: '/report',   icon: REPORTS_ICON },
    { name: 'Devices',     path: '/devices',  icon: DEVICES_ICON },
    { name: 'Payments',    path: '/pagos',    icon: PAYMENTS_ICON },
    { name: 'Rewards',     path: '/recompensas', icon: REWARDS_ICON },
    { name: 'Funds',       path: '/fondos',   icon: FUNDS_ICON },

    {
        name: 'Commerce',
        icon: COMMERCE_ICON,
        subItems: [
            { name: 'Register Commerce', path: '/registercommerce' },
            { name: 'Validate Commerces (Admin)', path: '/validatecommerce' },
            { name: 'View Commerces',    path: '/commerces' },
        ],
    },
];

export const otherItems: NavItem[] = [
    {
        name: 'Authentication',
        icon: AUTH_ICON,
        subItems: [
            { name: 'Sign In',          path: '/auth/login' },
            { name: 'Register',         path: '/auth/register' },
        ],
    },
];