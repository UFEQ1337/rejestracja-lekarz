// Warstwa widoku: wyłącznie budowanie elementów DOM na podstawie przekazanych danych.
// Nie wywołuje API - akcje użytkownika przekazuje do funkcji podanych przez app.js.

const STATUS_LABELS = {BOOKED: 'zarezerwowana', COMPLETED: 'odbyta', CANCELLED: 'anulowana'};
const ROLE_LABELS = {PATIENT: 'pacjent', RECEPTIONIST: 'rejestracja', DOCTOR: 'lekarz'};

const dateFormat = new Intl.DateTimeFormat('pl-PL', {weekday: 'long', day: 'numeric', month: 'long'});
const timeFormat = new Intl.DateTimeFormat('pl-PL', {hour: '2-digit', minute: '2-digit'});
const dateTimeFormat = new Intl.DateTimeFormat('pl-PL', {dateStyle: 'medium', timeStyle: 'short'});

function element(tag, options = {}, children = []) {
    const node = document.createElement(tag);
    if (options.className) node.className = options.className;
    if (options.text !== undefined) node.textContent = options.text;
    if (options.onClick) node.addEventListener('click', options.onClick);
    if (options.type) node.type = options.type;
    children.forEach(child => node.append(child));
    return node;
}

function emptyRow(columns, text) {
    const cell = element('td', {className: 'empty', text});
    cell.colSpan = columns;
    return element('tr', {}, [cell]);
}

export function showMessage(text, type = 'success') {
    const box = document.getElementById('message');
    box.textContent = text;
    box.className = `message ${type}`;
}

export function hideMessage() {
    document.getElementById('message').className = 'message hidden';
}

export function showError(error) {
    const details = error.violations?.map(v => `${v.field}: ${v.message}`).join('; ');
    showMessage(details ? `${error.message} (${details})` : error.message, 'error');
}

export function showLoggedIn(user) {
    document.getElementById('login-view').classList.add('hidden');
    document.getElementById('app-view').classList.remove('hidden');
    document.getElementById('user-box').classList.remove('hidden');
    document.getElementById('user-name').textContent = `${user.displayName} (${ROLE_LABELS[user.role]})`;
    const title = user.role === 'PATIENT' ? 'Moje wizyty' : 'Wizyty';
    document.getElementById('appointments-tab-btn').textContent = title;
    document.getElementById('appointments-title').textContent = title;
    document.getElementById('patient-select-row').classList.toggle('hidden', user.role !== 'RECEPTIONIST');
}

export function showLoggedOut() {
    document.getElementById('login-view').classList.remove('hidden');
    document.getElementById('app-view').classList.add('hidden');
    document.getElementById('user-box').classList.add('hidden');
}

export function switchTab(tabId) {
    document.querySelectorAll('.tab').forEach(tab => tab.classList.toggle('active', tab.dataset.tab === tabId));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.toggle('hidden', content.id !== tabId));
}

export function renderSpecializations(specializations) {
    const select = document.getElementById('specialization-filter');
    select.replaceChildren(element('option', {text: 'wszystkie'}));
    select.firstChild.value = '';
    specializations.forEach(s => {
        const option = element('option', {text: s.name});
        option.value = s.id;
        select.append(option);
    });
}

export function renderDoctors(doctors, selectedId, onShowSlots) {
    const body = document.getElementById('doctor-list');
    if (doctors.length === 0) {
        body.replaceChildren(emptyRow(4, 'Brak lekarzy o wybranej specjalizacji'));
        return;
    }
    body.replaceChildren(...doctors.map(doctor => {
        const row = element('tr', {className: doctor.id === selectedId ? 'selected' : ''}, [
            element('td', {text: `lek. ${doctor.fullName}`}),
            element('td', {text: doctor.specializations.map(s => s.name).join(', ')}),
            element('td', {text: doctor.room ?? '-'}),
            element('td', {}, [element('button', {
                className: 'btn btn-light btn-small', type: 'button', text: 'Pokaż terminy',
                onClick: () => onShowSlots(doctor)
            })])
        ]);
        return row;
    }));
}

export function renderSlots(doctor, slots, selectedSlotId, onSelect) {
    document.getElementById('slots-panel').classList.remove('hidden');
    document.getElementById('slots-title').textContent = `Wolne terminy - lek. ${doctor.fullName}`;
    const container = document.getElementById('slot-list');
    if (slots.length === 0) {
        container.replaceChildren(element('p', {className: 'empty', text: 'Brak wolnych terminów w najbliższych dwóch tygodniach'}));
        return;
    }
    const byDay = new Map();
    slots.forEach(slot => {
        const day = slot.startTime.substring(0, 10);
        if (!byDay.has(day)) byDay.set(day, []);
        byDay.get(day).push(slot);
    });
    container.replaceChildren(...[...byDay.entries()].map(([day, daySlots]) =>
        element('div', {className: 'slot-day'}, [
            element('h3', {text: dateFormat.format(new Date(day + 'T00:00:00'))}),
            element('div', {className: 'slots'}, daySlots.map(slot => element('button', {
                className: slot.id === selectedSlotId ? 'slot selected' : 'slot',
                type: 'button',
                text: timeFormat.format(new Date(slot.startTime)),
                onClick: () => onSelect(slot)
            })))
        ])));
}

export function hideSlots() {
    document.getElementById('slots-panel').classList.add('hidden');
}

export function renderBookingForm(slot) {
    document.getElementById('booking-panel').classList.remove('hidden');
    document.getElementById('booking-summary').textContent =
        `Lekarz: lek. ${slot.doctorName}, termin: ${dateTimeFormat.format(new Date(slot.startTime))}`;
}

export function hideBookingForm() {
    document.getElementById('booking-panel').classList.add('hidden');
    document.getElementById('booking-reason').value = '';
}

export function renderPatientOptions(patients) {
    const select = document.getElementById('booking-patient');
    select.replaceChildren(...patients.map(p => {
        const option = element('option', {text: `${p.lastName} ${p.firstName} (PESEL ${p.pesel})`});
        option.value = p.id;
        return option;
    }));
}

export function readBookingForm() {
    return {
        reason: document.getElementById('booking-reason').value.trim() || null,
        patientId: Number(document.getElementById('booking-patient').value) || null
    };
}

export function renderAppointments(appointments, now, onCancel) {
    const body = document.getElementById('appointment-list');
    if (appointments.length === 0) {
        body.replaceChildren(emptyRow(7, 'Brak wizyt'));
        return;
    }
    body.replaceChildren(...appointments.map(a => {
        const canCancel = a.status === 'BOOKED' && new Date(a.startTime) > now && onCancel;
        const actionCell = element('td');
        if (canCancel) {
            actionCell.append(element('button', {
                className: 'btn btn-danger btn-small', type: 'button', text: 'Anuluj',
                onClick: () => onCancel(a)
            }));
        }
        return element('tr', {}, [
            element('td', {className: 'nowrap', text: dateTimeFormat.format(new Date(a.startTime))}),
            element('td', {text: `lek. ${a.doctorName}`}),
            element('td', {text: a.patientName}),
            element('td', {text: a.room ?? '-'}),
            element('td', {text: a.reason ?? '-'}),
            element('td', {}, [element('span', {className: `status status-${a.status}`, text: STATUS_LABELS[a.status]})]),
            actionCell
        ]);
    }));
}
