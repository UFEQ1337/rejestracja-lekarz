// Kontroler klienta: obsługuje zdarzenia, przechowuje stan widoku i łączy api.js z views.js.

import * as api from './api.js';
import * as views from './views.js';

const state = {
    user: null,
    doctors: [],
    selectedDoctor: null,
    slots: [],
    selectedSlot: null
};

async function run(action) {
    try {
        await action();
    } catch (error) {
        if (error.status === 401) {
            logout();
        }
        views.showError(error);
    }
}

async function start() {
    bindEvents();
    if (!api.getToken()) {
        views.showLoggedOut();
        return;
    }
    await run(async () => {
        state.user = await api.fetchCurrentUser();
        await enterApp();
    });
}

async function enterApp() {
    views.showLoggedIn(state.user);
    views.renderSpecializations(await api.fetchSpecializations());
    if (state.user.role === 'RECEPTIONIST') {
        views.renderPatientOptions(await api.fetchPatients());
    }
    await loadDoctors();
    await loadAppointments();
}

async function loadDoctors() {
    const specializationId = document.getElementById('specialization-filter').value;
    state.doctors = await api.fetchDoctors(specializationId);
    views.renderDoctors(state.doctors, state.selectedDoctor?.id, showSlots);
}

async function showSlots(doctor) {
    await run(async () => {
        state.selectedDoctor = doctor;
        state.selectedSlot = null;
        state.slots = await api.fetchFreeSlots(doctor.id);
        views.hideBookingForm();
        views.renderDoctors(state.doctors, doctor.id, showSlots);
        views.renderSlots(doctor, state.slots, null, selectSlot);
    });
}

function selectSlot(slot) {
    state.selectedSlot = slot;
    views.renderSlots(state.selectedDoctor, state.slots, slot.id, selectSlot);
    views.renderBookingForm(slot);
}

async function book(event) {
    event.preventDefault();
    if (!state.selectedSlot) {
        return;
    }
    await run(async () => {
        const form = views.readBookingForm();
        const patientId = state.user.role === 'RECEPTIONIST' ? form.patientId : null;
        const appointment = await api.bookAppointment(state.selectedSlot.id, form.reason, patientId);
        views.hideBookingForm();
        views.showMessage(`Zarezerwowano wizytę u lek. ${appointment.doctorName}, `
            + `${appointment.startTime.replace('T', ' ').substring(0, 16)}.`);
        await showSlots(state.selectedDoctor);
        await loadAppointments();
    });
}

async function loadAppointments() {
    const status = document.getElementById('status-filter').value;
    const appointments = await api.fetchAppointments(status);
    const canCancel = state.user.role !== 'DOCTOR' ? cancel : null;
    views.renderAppointments(appointments, new Date(), canCancel);
}

async function cancel(appointment) {
    if (!confirm(`Anulować wizytę u lek. ${appointment.doctorName}?`)) {
        return;
    }
    await run(async () => {
        await api.cancelAppointment(appointment.id);
        views.showMessage('Wizyta została anulowana, termin jest znowu wolny.');
        await loadAppointments();
        if (state.selectedDoctor) {
            await showSlots(state.selectedDoctor);
        }
    });
}

async function login(event) {
    event.preventDefault();
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;
    await run(async () => {
        state.user = await api.login(username, password);
        views.hideMessage();
        await enterApp();
    });
}

function logout() {
    api.clearToken();
    Object.assign(state, {user: null, doctors: [], selectedDoctor: null, slots: [], selectedSlot: null});
    views.hideSlots();
    views.hideBookingForm();
    views.switchTab('doctors-tab');
    views.showLoggedOut();
}

function bindEvents() {
    document.getElementById('login-form').addEventListener('submit', login);
    document.getElementById('logout-btn').addEventListener('click', () => {
        logout();
        views.hideMessage();
    });
    document.getElementById('booking-form').addEventListener('submit', book);
    document.getElementById('booking-cancel').addEventListener('click', () => {
        state.selectedSlot = null;
        views.hideBookingForm();
        views.renderSlots(state.selectedDoctor, state.slots, null, selectSlot);
    });
    document.getElementById('specialization-filter').addEventListener('change', () => run(loadDoctors));
    document.getElementById('status-filter').addEventListener('change', () => run(loadAppointments));
    document.querySelectorAll('.tab').forEach(tab =>
        tab.addEventListener('click', () => views.switchTab(tab.dataset.tab)));
}

start();
