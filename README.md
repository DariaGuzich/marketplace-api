# marketplace-api

REST API настроек маркетплейса. Источник данных и **источник OpenAPI-спецификации** (`openapi.json`)
для всей системы: по ней `marketplace-bff` генерирует свои типы.

```
marketplace-ui  →  marketplace-bff  →  marketplace-api (этот репозиторий)
```

## API

| Метод | Путь | Ответ |
|---|---|---|
| GET | `/accounts/{account_id}/settings` | 200 и настройки; 404, если настройки ещё не сохранены |
| PUT | `/accounts/{account_id}/settings` | 200 и сохранённые настройки; 400, если не хватает поля |

PUT полностью заменяет настройки, все три поля обязательны:

```json
{ "floor_price": 1.5, "currency": "USD", "blocked_domains": ["bad.com"] }
```

Поля в snake_case намеренно: в BFF видно преобразование в camelCase.

## Запуск

Нужны Java 21 и Maven.

```bash
mvn spring-boot:run                 # порт 8080
PORT=9090 mvn spring-boot:run       # другой порт (bash)
$env:PORT=9090; mvn spring-boot:run # другой порт (PowerShell)
```

API запускается первым: BFF ходит в него, а UI ходит в BFF.

Проверка, что всё работает:

```bash
curl -i http://localhost:8080/accounts/acc-1/settings        # 404: настроек ещё нет
curl -i -X PUT http://localhost:8080/accounts/acc-1/settings \
     -H "Content-Type: application/json" \
     -d '{"floor_price": 1.5, "currency": "USD", "blocked_domains": ["bad.com"]}'
curl -i http://localhost:8080/accounts/acc-1/settings        # 200 и сохранённые настройки
curl http://localhost:8080/v3/api-docs                       # живая OpenAPI-спецификация
```

Данные хранятся в памяти и пропадают при перезапуске.

## Тесты

```bash
mvn test      # только тесты
mvn verify    # тесты + перегенерация openapi.json
```

- `SettingsServiceTest` — юнит-тест: сервис с настоящим in-memory репозиторием, без Spring.
- `SettingsApiTest` — API-тесты: приложение поднимается целиком, запросы идут по HTTP.

**Почему RestAssured, а не MockMvc.** MockMvc вызывает контроллеры внутри того же процесса без
настоящего HTTP-сервера. RestAssured отправляет настоящие HTTP-запросы на запущенное приложение, как
это будет делать BFF. Для проекта про границы между сервисами нам важно проверять именно то, что видно
снаружи: пути, статусы, JSON в snake_case. MockMvc был бы быстрее, но это различие здесь не важно.

## OpenAPI-спецификация

`openapi.json` в корне **генерируется из кода**, руками его не правят.

```bash
mvn verify
```

Как это работает (см. `pom.xml`):
1. `spring-boot-maven-plugin` (goal `start`) поднимает приложение на порту 18080;
2. `springdoc-openapi-maven-plugin` скачивает `http://localhost:18080/v3/api-docs` в `openapi.json`;
3. `spring-boot-maven-plugin` (goal `stop`) останавливает приложение.

После любого изменения контроллеров или модели запусти `mvn verify` и закоммить `openapi.json`
вместе с кодом. Иначе CI упадёт (см. ниже).

Имена полей в JSON задаются аннотацией `@JsonProperty` на `Settings`. Глобальная настройка
`spring.jackson.property-naming-strategy` здесь не подходит: Spring Boot 4 сериализует JSON через
Jackson 3, а springdoc строит схему через Jackson 2. Глобальную стратегию видел бы только Spring, и
спецификация описывала бы `floorPrice`, хотя API отдаёт `floor_price`. Аннотацию `@JsonProperty`
читают обе версии Jackson.

## CI (`.github/workflows/ci.yml`)

| Job | Когда | Что проверяет |
|---|---|---|
| `test` | push в main, PR, вручную | `mvn verify` (тесты + генерация), затем `git diff --exit-code openapi.json` |
| `breaking-changes` | только PR | oasdiff сравнивает `openapi.json` ветки PR с `openapi.json` из main |

## Инструменты: зачем каждый

- **Spring Boot** — веб-сервер, JSON, валидация, внедрение зависимостей. Без него пришлось бы писать много инфраструктурного кода.
- **Jakarta Validation (`@NotNull`, `@Valid`)** — возвращает 400 на неполный запрос и одновременно помечает поля как `required` в спецификации.
- **springdoc-openapi** — строит OpenAPI-спецификацию по контроллерам и моделям. Проблема, которую решает: спецификация, написанная руками, со временем перестаёт совпадать с кодом. Здесь её источник — сам код.
- **springdoc-openapi-maven-plugin** — сохраняет спецификацию в файл одной командой, чтобы её можно было коммитить, смотреть в diff и отдавать потребителям.
- **Проверка `git diff --exit-code openapi.json`** — ловит ситуацию «код поменяли, спецификацию не перегенерировали». Без неё потребители (BFF) получили бы устаревший контракт из main.
- **oasdiff** — знает правила совместимости OpenAPI: удалить поле ответа, сделать поле запроса обязательным или сменить тип — это breaking change, а добавить необязательное поле — нет. Проблема, которую решает: CI этого репозитория зелёный (тесты API проверяют только сам API), а потребитель при этом ломается. `fail-on: ERR` — падать только на точно ломающих изменениях (уровень WARN — «возможно ломающие»). `review: false` — не загружать спецификации на внешний сервис oasdiff.com.
- **JUnit 5 + AssertJ** — юнит-тесты. **RestAssured** — API-тесты (почему именно он — см. выше).

## Куда встроится то, что будет позже

- **PostgreSQL** — новая реализация `SettingsRepository` рядом с `InMemorySettingsRepository`. Контроллер и сервис не меняются. Интеграционные тесты с Testcontainers проверят именно эту реализацию.
- **Auth и tenancy** — аутентификация будет фильтром Spring Security перед контроллером, а проверка «пользователь имеет доступ к этому account_id» — в `SettingsService`.
- **Config publisher** — вызов после сохранения в `SettingsService.update`.
- **Pact** — BFF опубликует контракт своих ожиданий, а этот репозиторий будет проверять его в CI (provider verification).

## Эксперименты

См. [EXPERIMENTS.md](EXPERIMENTS.md).
