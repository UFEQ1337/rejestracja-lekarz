-- Dane demonstracyjne: specjalizacje, lekarze, pacjenci i konta.
-- Wszystkie dane osobowe poza autorem projektu sa fikcyjne (PESEL-e nie sa prawdziwe).
-- Hasla kont demo: pacjenci Pacjent123!, rejestracja Rejestracja123!, lekarze Lekarz123!

INSERT INTO specialization (name) VALUES
    ('Choroby wewnętrzne'),
    ('Dermatologia'),
    ('Kardiologia'),
    ('Medycyna rodzinna'),
    ('Okulistyka'),
    ('Ortopedia'),
    ('Pediatria');

INSERT INTO doctor (first_name, last_name, license_number, email, phone, room) VALUES
    ('Anna', 'Kowalska', '1234561', 'anna.kowalska@przychodnia-popielarz.pl', '+48 61 100 01 01', '101'),
    ('Piotr', 'Nowak', '2345672', 'piotr.nowak@przychodnia-popielarz.pl', '+48 61 100 01 02', '102'),
    ('Katarzyna', 'Wiśniewska', '3456783', 'katarzyna.wisniewska@przychodnia-popielarz.pl', '+48 61 100 02 01', '201'),
    ('Tomasz', 'Wójcik', '4567894', 'tomasz.wojcik@przychodnia-popielarz.pl', '+48 61 100 01 05', '105'),
    ('Magdalena', 'Kamińska', '5678905', 'magdalena.kaminska@przychodnia-popielarz.pl', '+48 61 100 02 03', '203'),
    ('Jan', 'Lewandowski', '6789016', 'jan.lewandowski@przychodnia-popielarz.pl', '+48 61 100 02 04', '204');

INSERT INTO doctor_specialization (doctor_id, specialization_id)
SELECT d.id, s.id FROM doctor d, specialization s WHERE
       (d.license_number = '1234561' AND s.name IN ('Kardiologia', 'Choroby wewnętrzne'))
    OR (d.license_number = '2345672' AND s.name = 'Medycyna rodzinna')
    OR (d.license_number = '3456783' AND s.name IN ('Pediatria', 'Medycyna rodzinna'))
    OR (d.license_number = '4567894' AND s.name = 'Ortopedia')
    OR (d.license_number = '5678905' AND s.name = 'Dermatologia')
    OR (d.license_number = '6789016' AND s.name = 'Okulistyka');

INSERT INTO patient (first_name, last_name, pesel, email, phone, birth_date) VALUES
    ('Mateusz', 'Popielarz', '95031512345', 'mateusz.popielarz@example.com', '+48 600 100 200', DATE '1995-03-15'),
    ('Joanna', 'Zielińska', '88072254321', 'joanna.zielinska@example.com', '+48 600 200 300', DATE '1988-07-22'),
    ('Krzysztof', 'Szymański', '76110398765', 'krzysztof.szymanski@example.com', '+48 600 300 400', DATE '1976-11-03'),
    ('Ewa', 'Dąbrowska', '01250811223', 'ewa.dabrowska@example.com', '+48 600 400 500', DATE '2001-05-08');

INSERT INTO app_user (username, password_hash, role, patient_id, doctor_id) VALUES
    ('mpopielarz', '$2a$10$0KJx7y6nQTWqSH13oNkYteDOCgjDI6L3a1TkvmLJIx1tO/I6/2eAC', 'PATIENT',
        (SELECT id FROM patient WHERE pesel = '95031512345'), NULL),
    ('jzielinska', '$2a$10$0KJx7y6nQTWqSH13oNkYteDOCgjDI6L3a1TkvmLJIx1tO/I6/2eAC', 'PATIENT',
        (SELECT id FROM patient WHERE pesel = '88072254321'), NULL),
    ('rejestracja', '$2a$10$Ryakz5710ef.YmmxRcZbX.U9mZdGEPWp/bV90SLySicdT0cxgrunO', 'RECEPTIONIST', NULL, NULL),
    ('akowalska', '$2a$10$0jSZ/r5.av5Uh59tTXblTeY85Q1X4EayT6nlmdezo5X7AOiXFnBk2', 'DOCTOR',
        NULL, (SELECT id FROM doctor WHERE license_number = '1234561')),
    ('pnowak', '$2a$10$0jSZ/r5.av5Uh59tTXblTeY85Q1X4EayT6nlmdezo5X7AOiXFnBk2', 'DOCTOR',
        NULL, (SELECT id FROM doctor WHERE license_number = '2345672'));
