export interface Device {
    id: number;
    userId: number;
    fingerprint: string;
    type: string;
    os: string;
    browser: string;
    registeredAt: string;
    lastLoginAt?: string | null;
}

export interface DeviceLog {
    id?: number;
    userId: number;
    deviceId?: number | null;

    action: string;
    result: string;
    details?: string | null;
    createdAt: string;

    deviceFingerprintSnapshot?: string | null;
    deviceOsSnapshot?: string | null;
    deviceTypeSnapshot?: string | null;
    deviceBrowserSnapshot?: string | null;
}
