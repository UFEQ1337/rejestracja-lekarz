# Dokumentacja REST API

Bazowy adres: `http://localhost:8080/api/v1`. Format danych: JSON (UTF-8), daty w ISO 8601 (`2026-11-02T09:00:00`, czas lokalny Europe/Warsaw).

Dokumentacja interaktywna (OpenAPI 3.1, generowana z kodu przez springdoc):

* Swagger UI: <http://localhost:8080/swagger-ui.html>
* specyfikacja JSON: <http://localhost:8080/v3/api-docs> (kopia w repozytorium: [`openapi.json`](openapi.json))

## Uwierzytelnianie

1. `POST /api/v1/auth/login` z loginem i hasłem zwraca `accessToken` (JWT).
2. Kolejne żądania wysyłają nagłówek `Authorization: Bearer <accessToken>`.
3. API jest bezstanowe: serwer nie tworzy sesji HTTP ani ciasteczek.

Konta demo: `mpopielarz` / `Pacjent123!` (pacjent Mateusz Popielarz), `jzielinska` / `Pacjent123!`,
`rejestracja` / `Rejestracja123!`, `akowalska` i `pnowak` / `Lekarz123!` (lekarze).

## Konwencje

* zasoby jako rzeczowniki w liczbie mnogiej, wersja w ścieżce: `/api/v1/doctors`, `/api/v1/appointments`,
* zasoby podrzędne: `/doctors/{id}/slots`, `/patients/{id}/appointments`,
* `GET` – odczyt, `POST` – utworzenie (201 + nagłówek `Location`), `PUT` – pełna aktualizacja,
  `PATCH` – zmiana części zasobu (status wizyty), `DELETE` – usunięcie/wycofanie (204),
* filtrowanie przez parametry zapytania (`?specializationId=3`, `?status=BOOKED&from=2026-11-01`).

## Lista endpointów

| Metoda | Ścieżka | Opis | Dostęp | Kody odpowiedzi |
|---|---|---|---|---|
| POST | `/auth/login` | logowanie, zwraca token JWT | publiczny | 200, 400, 401 |
| POST | `/auth/register` | rejestracja pacjenta z kontem | publiczny | 201, 400, 409 |
| GET | `/auth/me` | dane zalogowanego użytkownika | zalogowany | 200, 401 |
| GET | `/specializations` | lista specjalizacji | publiczny | 200 |
| POST | `/specializations` | dodanie specjalizacji | rejestracja | 201, 400, 401, 403, 409 |
| GET | `/doctors` | lista lekarzy, filtr `specializationId` | publiczny | 200 |
| GET | `/doctors/{id}` | szczegóły lekarza | publiczny | 200, 404 |
| POST | `/doctors` | dodanie lekarza | rejestracja | 201, 400, 401, 403, 409 |
| PUT | `/doctors/{id}` | aktualizacja lekarza | rejestracja | 200, 400, 403, 404, 409 |
| DELETE | `/doctors/{id}` | wycofanie lekarza | rejestracja | 204, 403, 404, 409 |
| GET | `/doctors/{id}/slots` | terminy lekarza (`from`, `to`, `available`) | publiczny | 200, 400, 404 |
| POST | `/doctors/{id}/slots` | dodanie terminu | rejestracja, lekarz (swój grafik) | 201, 400, 403, 404, 409 |
| GET | `/slots/{id}` | szczegóły terminu | zalogowany | 200, 404 |
| DELETE | `/slots/{id}` | usunięcie wolnego terminu | rejestracja, lekarz (swój grafik) | 204, 403, 404, 409 |
| GET | `/patients` | lista pacjentów, filtr `lastName` | rejestracja, lekarz | 200, 403 |
| GET | `/patients/{id}` | dane pacjenta | zalogowany (pacjent tylko swoje) | 200, 403, 404 |
| POST | `/patients` | dodanie pacjenta bez konta | rejestracja | 201, 400, 403, 409 |
| PUT | `/patients/{id}` | aktualizacja danych pacjenta | rejestracja, pacjent (swoje) | 200, 400, 403, 404, 409 |
| GET | `/patients/{id}/appointments` | wizyty pacjenta | zalogowany (pacjent tylko swoje) | 200, 403, 404 |
| GET | `/appointments` | wyszukiwanie wizyt (`patientId`, `doctorId`, `status`, `from`, `to`) | zalogowany | 200, 400, 401, 403 |
| GET | `/appointments/{id}` | szczegóły wizyty | zalogowany (właściciel) | 200, 403, 404 |
| POST | `/appointments` | rezerwacja wizyty | pacjent, rejestracja | 201, 400, 401, 403, 404, 409 |
| PATCH | `/appointments/{id}` | zmiana statusu: `CANCELLED` lub `COMPLETED` | pacjent, rejestracja, lekarz | 200, 400, 403, 404, 409 |

## Format błędu

Wszystkie błędy (także 401 i 403 z warstwy Spring Security) mają tę samą strukturę:

```json
{
  "timestamp": "2026-10-31T21:03:46.116Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Błąd walidacji danych wejściowych",
  "path": "/api/v1/doctors",
  "violations": [ { "field": "licenseNumber", "message": "numer prawa wykonywania zawodu musi mieć 7 cyfr" } ]
}
```

| Kod | Kiedy |
|---|---|
| 200 OK | poprawny odczyt lub aktualizacja |
| 201 Created | utworzono zasób, nagłówek `Location` wskazuje jego adres |
| 204 No Content | usunięto / wycofano zasób |
| 400 Bad Request | błąd walidacji, zły format JSON lub parametru, naruszona reguła danych wejściowych |
| 401 Unauthorized | brak lub nieważny token, złe hasło |
| 403 Forbidden | rola bez uprawnień albo próba dostępu do cudzych danych |
| 404 Not Found | brak zasobu o podanym id |
| 405 Method Not Allowed | metoda nieobsługiwana dla zasobu |
| 409 Conflict | termin zajęty lub miniony, nakładające się terminy, duplikat PESEL/PWZ/e-maila, wizyta już anulowana |
| 415 Unsupported Media Type | treść inna niż `application/json` |
| 500 Internal Server Error | nieobsłużony błąd serwera (zapisywany w logu) |

## Przykładowe żądania i odpowiedzi

Poniższe przykłady zostały wykonane na uruchomionej aplikacji (PostgreSQL, dane demo).

### Logowanie pacjenta

```http
POST /api/v1/auth/login
{"username": "mpopielarz", "password": "Pacjent123!"}


HTTP 200
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJ...",
  "tokenType": "Bearer",
  "expiresAt": "2026-11-01T05:03:45.982403900Z",
  "user": {
    "id": 1,
    "username": "mpopielarz",
    "role": "PATIENT",
    "displayName": "Mateusz Popielarz",
    "patientId": 1,
    "doctorId": null
  }
}
```

### Lista lekarzy z filtrem specjalizacji (publiczne)

```http
GET /api/v1/doctors?specializationId=3

HTTP 200
[
  {
    "id": 1,
    "firstName": "Anna",
    "lastName": "Kowalska",
    "fullName": "Anna Kowalska",
    "licenseNumber": "1234561",
    "email": "anna.kowalska@przychodnia-popielarz.pl",
    "phone": "+48 61 100 01 01",
    "room": "101",
    "specializations": [
      {
        "id": 1,
        "name": "Choroby wewnętrzne"
      },
      {
        "id": 3,
        "name": "Kardiologia"
      }
    ]
  }
]
```

### Wolne terminy lekarza (publiczne)

```http
GET /api/v1/doctors/2/slots

HTTP 200
[
  {
    "id": 241,
    "doctorId": 2,
    "doctorName": "Piotr Nowak",
    "startTime": "2026-11-02T12:00:00",
    "endTime": "2026-11-02T12:20:00",
    "available": true
  },
  {
    "id": 243,
    "doctorId": 2,
    "doctorName": "Piotr Nowak",
    "startTime": "2026-11-02T12:40:00",
    "endTime": "2026-11-02T13:00:00",
    "available": true
  }
]
```

### Rezerwacja wizyty

```http
POST /api/v1/appointments
Authorization: Bearer <token mpopielarz>
{"slotId": 241, "reason": "Przegląd okresowy"}

HTTP 201
Location: http://localhost:8080/api/v1/appointments/152
{
  "id": 152,
  "status": "BOOKED",
  "reason": "Przegląd okresowy",
  "createdAt": "2026-10-31T22:03:46",
  "cancelledAt": null,
  "slotId": 241,
  "startTime": "2026-11-02T12:00:00",
  "endTime": "2026-11-02T12:20:00",
  "doctorId": 2,
  "doctorName": "Piotr Nowak",
  "room": "102",
  "patientId": 1,
  "patientName": "Mateusz Popielarz"
}
```

### Ponowna rezerwacja tego samego terminu

```http
POST /api/v1/appointments
Authorization: Bearer <token rejestracja>
{"slotId": 241, "patientId": 3}

HTTP 409
{
  "timestamp": "2026-10-31T21:03:46.080Z",
  "status": 409,
  "error": "Conflict",
  "message": "Wybrany termin jest już zajęty",
  "path": "/api/v1/appointments"
}
```

### Anulowanie wizyty

```http
PATCH /api/v1/appointments/152
Authorization: Bearer <token mpopielarz>
{"status": "CANCELLED"}

HTTP 200
{
  "id": 152,
  "status": "CANCELLED",
  "reason": "Przegląd okresowy",
  "createdAt": "2026-10-31T22:03:46",
  "cancelledAt": "2026-10-31T22:03:46",
  "slotId": 241,
  "startTime": "2026-11-02T12:00:00",
  "endTime": "2026-11-02T12:20:00",
  "doctorId": 2,
  "doctorName": "Piotr Nowak",
  "room": "102",
  "patientId": 1,
  "patientName": "Mateusz Popielarz"
}
```

### Walidacja danych lekarza

```http
POST /api/v1/doctors
Authorization: Bearer <token rejestracja>
{"firstName": "", "lastName": "Mazur", "licenseNumber": "12", "email": "zly-adres", "specializationIds": []}

HTTP 400
{
  "timestamp": "2026-10-31T21:03:46.116Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Błąd walidacji danych wejściowych",
  "path": "/api/v1/doctors",
  "violations": [
    {
      "field": "licenseNumber",
      "message": "numer prawa wykonywania zawodu musi mieć 7 cyfr"
    },
    {
      "field": "email",
      "message": "musi być poprawnie sformatowanym adresem e-mail"
    },
    {
      "field": "firstName",
      "message": "nie może być odstępem"
    },
    {
      "field": "specializationIds",
      "message": "lekarz musi mieć co najmniej jedną specjalizację"
    }
  ]
}
```

### Nieistniejący zasób

```http
GET /api/v1/doctors/999

HTTP 404
{
  "timestamp": "2026-10-31T21:03:46.122Z",
  "status": 404,
  "error": "Not Found",
  "message": "Lekarz o id 999 nie istnieje",
  "path": "/api/v1/doctors/999"
}
```

### Brak tokena

```http
GET /api/v1/appointments

HTTP 401
{
  "timestamp": "2026-10-31T21:03:46.124Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Wymagane uwierzytelnienie tokenem Bearer",
  "path": "/api/v1/appointments"
}
```

### Pacjent próbuje odczytać dane innego pacjenta

```http
GET /api/v1/patients/2
Authorization: Bearer <token mpopielarz>

HTTP 403
{
  "timestamp": "2026-10-31T21:03:46.127Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Pacjent ma dostęp tylko do własnych danych",
  "path": "/api/v1/patients/2"
}
```

### Usunięcie (wycofanie) lekarza z przyszłymi wizytami

```http
DELETE /api/v1/doctors/1
Authorization: Bearer <token rejestracja>

HTTP 409
{
  "timestamp": "2026-10-31T21:03:46.135Z",
  "status": 409,
  "error": "Conflict",
  "message": "Lekarz ma zarezerwowane przyszłe wizyty - najpierw należy je anulować",
  "path": "/api/v1/doctors/1"
}
```
