// Warstwa komunikacji z REST API. Nie zna struktury strony - zwraca tylko dane
// albo rzuca ApiError z treścią odpowiedzi błędu serwera.

const BASE_URL = '/api/v1';
const TOKEN_KEY = 'przychodnia.token';

export class ApiError extends Error {
    constructor(status, body) {
        super(body?.message ?? `Błąd HTTP ${status}`);
        this.status = status;
        this.violations = body?.violations ?? [];
    }
}

export function getToken() {
    return sessionStorage.getItem(TOKEN_KEY);
}

export function clearToken() {
    sessionStorage.removeItem(TOKEN_KEY);
}

async function request(method, path, body) {
    const headers = {Accept: 'application/json'};
    const token = getToken();
    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }
    if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
    }
    const response = await fetch(BASE_URL + path, {
        method,
        headers,
        body: body !== undefined ? JSON.stringify(body) : undefined
    });
    if (response.status === 204) {
        return null;
    }
    const data = await response.json().catch(() => null);
    if (!response.ok) {
        throw new ApiError(response.status, data);
    }
    return data;
}

function query(params) {
    const entries = Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== '');
    return entries.length ? '?' + new URLSearchParams(entries).toString() : '';
}

export async function login(username, password) {
    const result = await request('POST', '/auth/login', {username, password});
    sessionStorage.setItem(TOKEN_KEY, result.accessToken);
    return result.user;
}

export const fetchCurrentUser = () => request('GET', '/auth/me');
export const fetchSpecializations = () => request('GET', '/specializations');
export const fetchDoctors = (specializationId) => request('GET', '/doctors' + query({specializationId}));
export const fetchFreeSlots = (doctorId) => request('GET', `/doctors/${doctorId}/slots?available=true`);
export const fetchPatients = () => request('GET', '/patients');
export const fetchAppointments = (status) => request('GET', '/appointments' + query({status}));
export const bookAppointment = (slotId, reason, patientId) =>
    request('POST', '/appointments', {slotId, reason, patientId});
export const cancelAppointment = (id) => request('PATCH', `/appointments/${id}`, {status: 'CANCELLED'});
