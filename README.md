# Przychodnia Popielarz – system rejestracji do lekarza

Projekt zaliczeniowy z przedmiotu **Języki obiektowe 2 (Java)**, WSB Merito Poznań, rok akad. 2026/27.
Prowadzący: dr Marek Fechner. Autor: **Mateusz Popielarz**.

Temat nr 19: „Aplikacja wspierająca system rejestracji do lekarza”, **wariant I – REST API + warstwa kliencka**.

Aplikacja udostępnia REST API do rejestracji pacjentów na wizyty (lekarze, specjalizacje, grafik terminów,
wizyty, pacjenci) oraz prostego klienta WWW, z którego pacjent może wybrać lekarza, zarezerwować wolny termin,
przejrzeć swoje wizyty i je anulować. Rejestracja przychodni i lekarze mają własne uprawnienia.

## Technologie

Java 21, Spring Boot 3.5 (Web MVC, Data JPA, Security + JWT, Validation), PostgreSQL 17, H2, Flyway,
springdoc-openapi (Swagger UI), JUnit 5 + Mockito + MockMvc, Maven Wrapper, Docker Compose,
klient: HTML + CSS + JavaScript (`fetch`) bez frameworka.

## Uruchomienie

Wymagania: JDK 21 lub nowszy (`JAVA_HOME` musi wskazywać na JDK 21+), opcjonalnie Docker.
Maven nie jest potrzebny – projekt zawiera Maven Wrapper.

### Wariant 1 – PostgreSQL w Dockerze (docelowy)

```bash
docker compose up -d          # baza przychodnia na localhost:5432
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

### Wariant 2 – bez Dockera, baza H2 w pamięci (profil `dev`)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Przy starcie Flyway tworzy schemat i wczytuje dane demonstracyjne (grafik generowany względem bieżącej daty).

Po uruchomieniu:

| Adres | Co |
|---|---|
| <http://localhost:8080/> | klient WWW |
| <http://localhost:8080/swagger-ui.html> | Swagger UI (dokumentacja i testowanie API) |
| <http://localhost:8080/v3/api-docs> | specyfikacja OpenAPI w JSON |

Zatrzymanie bazy: `docker compose down` (z `-v`, jeśli dane mają zostać usunięte).

### Konta demonstracyjne

| Login | Hasło | Rola |
|---|---|---|
| `mpopielarz` | `Pacjent123!` | pacjent (Mateusz Popielarz) |
| `jzielinska` | `Pacjent123!` | pacjent |
| `rejestracja` | `Rejestracja123!` | rejestracja przychodni |
| `akowalska` | `Lekarz123!` | lekarz (Anna Kowalska) |
| `pnowak` | `Lekarz123!` | lekarz (Piotr Nowak) |

Dane pacjentów i lekarzy (poza autorem) są fikcyjne.

### Konfiguracja

| Zmienna | Domyślnie | Znaczenie |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/przychodnia` | adres bazy |
| `DB_USER` / `DB_PASSWORD` | `przychodnia` / `przychodnia` | dane logowania do bazy |
| `JWT_SECRET` | klucz deweloperski | klucz HMAC do podpisu tokenów (min. 32 znaki) – w produkcji obowiązkowo zmienić |

## Testy

```bash
./mvnw test
```

34 testy: jednostkowe serwisów (JUnit 5 + Mockito), testy kontrolerów (`@WebMvcTest` + MockMvc)
i test integracyjny całego przepływu rezerwacji na bazie H2. Testy nie wymagają Dockera.

## API – skrót

Pełny opis: [docs/api.md](docs/api.md) oraz Swagger UI.

| Metoda | Ścieżka | Opis |
|---|---|---|
| POST | `/api/v1/auth/login` | logowanie, zwraca token JWT |
| POST | `/api/v1/auth/register` | rejestracja konta pacjenta |
| GET | `/api/v1/auth/me` | zalogowany użytkownik |
| GET, POST | `/api/v1/specializations` | specjalizacje |
| GET, POST | `/api/v1/doctors` | lista / dodanie lekarza |
| GET, PUT, DELETE | `/api/v1/doctors/{id}` | lekarz |
| GET, POST | `/api/v1/doctors/{id}/slots` | terminy lekarza |
| GET, DELETE | `/api/v1/slots/{id}` | pojedynczy termin |
| GET, POST | `/api/v1/patients` | kartoteka pacjentów |
| GET, PUT | `/api/v1/patients/{id}` | pacjent |
| GET | `/api/v1/patients/{id}/appointments` | wizyty pacjenta |
| GET, POST | `/api/v1/appointments` | wyszukiwanie / rezerwacja wizyty |
| GET, PATCH | `/api/v1/appointments/{id}` | wizyta, zmiana statusu (anulowanie, wizyta odbyta) |

Chronione endpointy wymagają nagłówka `Authorization: Bearer <token>`. API jest bezstanowe (bez sesji HTTP).

## Struktura repozytorium

```
rejestracja-lekarz/
├── docker-compose.yml           baza PostgreSQL
├── pom.xml, mvnw, mvnw.cmd      Maven + wrapper
├── docs/
│   ├── specyfikacja.md          opis, odbiorcy, wymagania, korzyści biznesowe
│   ├── dokumentacja_techniczna.md  stos, architektura, baza, klasy, testy
│   ├── api.md, openapi.json     dokumentacja REST API
│   └── diagramy/                diagramy PNG + źródła PlantUML
└── src/
    ├── main/java/pl/popielarz/rejestracja/
    │   ├── controller/  dto/  mapper/  service/  repository/  model/
    │   ├── exception/  security/  config/
    │   └── RejestracjaApplication.java
    ├── main/java/db/migration/  migracja Flyway w Javie (grafik demo)
    ├── main/resources/
    │   ├── application.yml, application-dev.yml
    │   ├── db/migration/        V1__schema.sql, V2__demo_data.sql
    │   └── static/              klient WWW (index.html, css/, js/api.js, js/views.js, js/app.js)
    └── test/java/...            testy
```

## Dokumentacja

* [Specyfikacja](docs/specyfikacja.md)
* [Dokumentacja techniczna](docs/dokumentacja_techniczna.md) – stos, architektura, diagram bazy, diagramy klas, testy
* [Dokumentacja API](docs/api.md)

## Autor

Mateusz Popielarz, WSB Merito Poznań, Informatyka, studia II stopnia.
