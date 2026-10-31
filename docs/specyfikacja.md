# Specyfikacja – Przychodnia Popielarz (system rejestracji do lekarza)

Autor: Mateusz Popielarz, WSB Merito Poznań, przedmiot „Języki obiektowe 2 (Java)”, prowadzący dr Marek Fechner.
Temat nr 19 z listy: „Aplikacja wspierająca system rejestracji do lekarza”, wariant I (REST API + warstwa kliencka).

## 1. Opis projektu

Przychodnia Popielarz to aplikacja do internetowej rejestracji pacjentów do lekarzy w niewielkiej przychodni.
Część serwerowa udostępnia REST API (`/api/v1`) napisane w Javie 21 i Spring Boot 3.5, dane są przechowywane
w relacyjnej bazie PostgreSQL. Pacjent może przejrzeć lekarzy i ich wolne terminy, zarezerwować wizytę,
sprawdzić swoje wizyty i je anulować. Rejestratorka zapisuje pacjentów (także tych bez konta), prowadzi
kartotekę, słownik specjalizacji i grafik lekarzy. Lekarz widzi swoje wizyty, dodaje terminy do własnego grafiku
i oznacza wizyty jako odbyte. Najważniejszą regułą biznesową jest to, że jeden termin może zostać
zarezerwowany tylko raz – próba podwójnej rezerwacji kończy się odpowiedzią `409 Conflict`.

Warstwę kliencką stanowi prosta strona WWW (HTML + CSS + JavaScript, `fetch`), która korzysta wyłącznie z API.

## 2. Odbiorcy systemu

| Odbiorca | Co robi w systemie |
|---|---|
| Pacjent | zakłada konto, przegląda lekarzy i wolne terminy, rezerwuje i anuluje wizyty, przegląda historię wizyt, poprawia swoje dane |
| Rejestratorka (rejestracja przychodni) | zapisuje pacjentów telefonicznie lub w okienku, prowadzi kartotekę pacjentów, słownik specjalizacji, dane lekarzy i ich grafiki |
| Lekarz | przegląda wizyty u siebie, dodaje i usuwa terminy we własnym grafiku, oznacza wizyty jako odbyte |
| Kierownik przychodni / inne systemy | korzysta z API (np. raport obłożenia lekarzy, aplikacja mobilna, przypomnienia SMS) |

## 3. Wymagania funkcjonalne

| Nr | Wymaganie | Realizacja (endpoint) |
|---|---|---|
| FR-01 | Przeglądanie listy lekarzy z możliwością filtrowania po specjalizacji (bez logowania). | `GET /api/v1/doctors?specializationId=` |
| FR-02 | Przeglądanie wolnych terminów wybranego lekarza w zadanym przedziale dat (domyślnie 14 dni). | `GET /api/v1/doctors/{id}/slots` |
| FR-03 | Rejestracja konta pacjenta oraz logowanie, które zwraca token JWT. | `POST /api/v1/auth/register`, `POST /api/v1/auth/login` |
| FR-04 | Rezerwacja wizyty w wolnym terminie: pacjent rezerwuje dla siebie, rejestratorka dla wskazanego pacjenta. | `POST /api/v1/appointments` |
| FR-05 | Ochrona przed błędnymi rezerwacjami: termin zajęty, termin miniony lub nakładający się na inną wizytę pacjenta jest odrzucany (409). | `POST /api/v1/appointments` |
| FR-06 | Lista wizyt z filtrami (status, lekarz, daty): pacjent widzi tylko swoje, lekarz swoje, rejestracja wszystkie. | `GET /api/v1/appointments`, `GET /api/v1/patients/{id}/appointments` |
| FR-07 | Anulowanie wizyty przez pacjenta lub rejestrację; anulowanie zwalnia termin. Nie można anulować wizyty, która się już zaczęła. | `PATCH /api/v1/appointments/{id}` (`CANCELLED`) |
| FR-08 | Oznaczenie wizyty jako odbytej przez lekarza (po jej rozpoczęciu). | `PATCH /api/v1/appointments/{id}` (`COMPLETED`) |
| FR-09 | Zarządzanie lekarzami (dodanie, edycja, wycofanie) i słownikiem specjalizacji przez rejestrację; lekarza z przyszłymi wizytami nie można wycofać. | `POST/PUT/DELETE /api/v1/doctors`, `POST /api/v1/specializations` |
| FR-10 | Zarządzanie grafikiem: dodawanie i usuwanie terminów; terminy jednego lekarza nie mogą się nakładać, zarezerwowanego terminu nie można usunąć. | `POST /api/v1/doctors/{id}/slots`, `DELETE /api/v1/slots/{id}` |
| FR-11 | Kartoteka pacjentów: lista i wyszukiwanie po nazwisku, dodanie pacjenta bez konta, edycja danych (pacjent tylko swoich). | `GET/POST/PUT /api/v1/patients` |

## 4. Wymagania pozafunkcjonalne

| Nr | Wymaganie | Jak spełnione |
|---|---|---|
| NFR-01 | Bezpieczeństwo: dostęp do danych osobowych tylko po uwierzytelnieniu i zgodnie z rolą. | JWT (HS256, ważność 8 h), hasła BCrypt, `@PreAuthorize` dla ról, kontrola właściciela w serwisach (403 przy próbie dostępu do cudzych danych). |
| NFR-02 | Bezstanowość API, możliwość uruchomienia kilku instancji serwera. | `SessionCreationPolicy.STATELESS`, brak sesji i ciasteczek, każde żądanie niesie token w nagłówku `Authorization`. |
| NFR-03 | Spójność danych przy równoczesnych rezerwacjach. | Transakcje `@Transactional`, ograniczenia w bazie (PK, FK, UNIQUE, CHECK), optimistic locking (`@Version`) na terminie – druga równoległa rezerwacja dostaje 409. |
| NFR-04 | Wydajność: lista wolnych terminów i wyszukiwanie wizyt poniżej 300 ms przy kilku tysiącach terminów. | Indeksy na `time_slot.start_time`, `appointment.patient_id/slot_id/status`, `doctor.last_name`; pobieranie powiązań przez `@EntityGraph` (bez problemu N+1). |
| NFR-05 | Łatwe uruchomienie i utrzymanie. | `docker compose up -d` + `./mvnw spring-boot:run`, migracje Flyway, profil `dev` z H2 bez Dockera, automatyczne testy, dokumentacja OpenAPI/Swagger. |
| NFR-06 | Czytelna obsługa błędów. | Jednolity format `ErrorResponse` (status, komunikat po polsku, ścieżka, lista błędnych pól). |

## 5. Korzyści biznesowe

1. **Odciążenie rejestracji** – większość pacjentów zapisuje się sama przez internet o dowolnej porze, rejestratorki mają mniej telefonów.
2. **Mniej pustych terminów** – anulowanie wizyty jednym kliknięciem od razu zwalnia termin dla kolejnych pacjentów.
3. **Brak pomyłek przy zapisach** – system nie pozwala zapisać dwóch osób na ten sam termin ani umówić pacjenta na dwie wizyty o tej samej godzinie.
4. **Możliwość rozwoju** – otwarte REST API pozwala dołączyć aplikację mobilną, przypomnienia SMS/e-mail lub raporty obłożenia lekarzy bez zmian w serwerze.

## 6. Role i uprawnienia (skrót)

| Operacja | Gość | Pacjent | Rejestracja | Lekarz |
|---|---|---|---|---|
| lekarze, specjalizacje, wolne terminy (odczyt) | tak | tak | tak | tak |
| rezerwacja wizyty | – | dla siebie | dla dowolnego pacjenta | – |
| anulowanie wizyty | – | swojej | każdej | – |
| oznaczenie wizyty jako odbytej | – | – | tak | swojej |
| lekarze i specjalizacje (zapis) | – | – | tak | – |
| terminy (dodanie / usunięcie) | – | – | tak | we własnym grafiku |
| kartoteka pacjentów | – | własne dane | tak | odczyt |
