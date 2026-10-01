# marketplace-api

REST API настроек маркетплейса. Источник данных и **источник OpenAPI-спецификации** (`openapi.json`)
для всей системы: по ней `marketplace-bff` генерирует свои типы.

```
marketplace-ui  →  marketplace-bff  →  marketplace-api (этот репозиторий)  →  PostgreSQL
```

## API

| Метод | Путь | Ответ |
|---|---|---|
| GET | `/accounts/{account_id}/settings` | 200 и настройки; 404, если настройки ещё не сохранены |
| PUT | `/accounts/{account_id}/settings` | 200 и сохранённые настройки; 400, если не хватает поля; 409, если `version` устарела |
| POST | `/accounts/{account_id}/blocked-domains` | 200 и обновлённые настройки; 404, если настроек нет; 409 при одновременном изменении |
| POST | `/accounts/{account_id}/settings/rollback?to_version=N` | 200 и новая версия со значениями версии N; 404, если нет настроек или такой версии |

PUT полностью заменяет настройки, три поля обязательны, `version` — нет:

```json
{ "floor_price": 1.5, "currency": "USD", "blocked_domains": ["bad.com"], "version": 3 }
```

Ответ — значения и текущая `version`:

```json
{ "floor_price": 1.5, "currency": "USD", "blocked_domains": ["bad.com"], "version": 4 }
```

POST добавляет один домен в конец списка (заголовок `Idempotency-Key` необязателен):

```json
{ "domain": "bad.com" }
```

**Откат** не возвращает старый номер версии, а создаёт **новую** версию со старыми значениями: версии только
растут. Новая версия пишется в outbox, как любое изменение, и доставляется в Serving. Serving применяет её,
потому что номер больше текущего.

Поля в snake_case намеренно: в BFF видно преобразование в camelCase.

## Идемпотентность и optimistic locking

**Идемпотентность.** PUT идемпотентен сам по себе: повтор того же запроса даёт тот же результат.
POST `/blocked-domains` — нет: каждый вызов добавляет домен, повтор (например, retry после таймаута)
добавит его ещё раз. Защита — заголовок `Idempotency-Key`:
- первый запрос с ключом выполняется, ответ сохраняется в таблицу `idempotency_keys` **в той же транзакции**,
  что и изменение настроек;
- повтор с тем же ключом не выполняет операцию, а возвращает сохранённый ответ.

Ключ генерирует клиент, один на одну логическую операцию: при retry он отправляет тот же ключ.

**Optimistic locking.** Клиент прочитал настройки (`version: 3`), поменял и отправил PUT с `"version": 3`.
Если за это время кто-то успел записать (`version` уже 4), API отвечает **409 Conflict**, и клиент должен
перечитать данные. Без этого вторая запись молча затирает первую — **lost update**.

Защита работает на двух уровнях:

| Уровень | Где | Ловит |
|---|---|---|
| проверка `version` клиента | `SettingsService.update` | клиент читал давно, кто-то записал после этого |
| `@Version` в Hibernate (`UPDATE ... WHERE version = <прочитанная>`) | `SettingsEntity` | два запроса одновременно прочитали одну версию; второй UPDATE обновит 0 строк → `ConflictExceptionHandler` → 409 |

Переменная `OPTIMISTIC_LOCKING_ENABLED=false` выключает **первый** уровень: `version` из запроса игнорируется,
и lost update воспроизводится последовательными запросами (см. EXPERIMENTS.md). Второй уровень (`@Version`)
выключить нельзя: одновременные записи Hibernate защищает всегда.

## Хранение: PostgreSQL, версии, outbox

```
src/main/java/com/example/marketplace/
  settings/  SettingsController → SettingsService → SettingsRepository (Spring Data JPA) → таблица settings
  outbox/    OutboxRepository → таблица outbox
  idempotency/  IdempotencyRepository → таблица idempotency_keys
src/main/resources/db/migration/   миграции Flyway: V1–V4
```

- **`version`** — колонка в `settings` с аннотацией `@Version`. Hibernate сам увеличивает её при каждом
  UPDATE. Первая сохранённая версия — 0. Если PUT пришёл с теми же значениями, UPDATE не выполняется и новой версии нет.
- **outbox** — при каждом изменении `SettingsService.update` в **одной транзакции** пишет новую версию
  в `settings` и запись в `outbox` (`account_id`, `version`, данные конфига в JSONB, статус `NEW`).
  Позже Config publisher будет читать из outbox записи со статусом `NEW` и доставлять их в Serving.

Зачем outbox. Наивный вариант — «сохранить в базу, затем отправить в Serving» — ломается между двумя шагами:
база сохранила, а отправка не случилась (или наоборот). С outbox есть только одна операция — транзакция в
своей базе, а доставку делает отдельный процесс, который может повторять её сколько угодно раз.

**Миграции.** Схему создаёт и меняет только Flyway: при старте приложения он применяет новые файлы из
`db/migration` по порядку и запоминает применённые в таблице `flyway_schema_history`. Hibernate
(`ddl-auto=validate`) схему не трогает. Он только проверяет, что сущности с ней совпадают, и не даёт
приложению стартовать, если нет.

| Миграция | Что делает |
|---|---|
| `V1__create_settings.sql` | таблица `settings` |
| `V2__add_settings_version.sql` | колонка `version`: **меняет существующие данные** — старые строки получают `version = 0` |
| `V3__create_outbox.sql` | таблица `outbox` |
| `V4__create_idempotency_keys.sql` | таблица `idempotency_keys` |

Применённую миграцию менять нельзя (Flyway сверяет контрольные суммы). Любое изменение — новый файл со следующим номером.

## Запуск

Нужны Java 21, Maven и запущенный Docker Desktop.

1. PostgreSQL: в [marketplace-infra](https://github.com/DariaGuzich/marketplace-infra) выполнить `docker compose up -d`.
2. API:
   ```bash
   mvn spring-boot:run                 # порт 8080
   PORT=9090 mvn spring-boot:run       # другой порт (bash)
   $env:PORT=9090; mvn spring-boot:run # другой порт (PowerShell)
   ```

| Переменная | По умолчанию |
|---|---|
| `PORT` | `8080` |
| `DB_URL` | `jdbc:postgresql://localhost:5432/marketplace` |
| `DB_USER` / `DB_PASSWORD` | `marketplace` / `marketplace` |
| `OPTIMISTIC_LOCKING_ENABLED` | `true`; `false` — игнорировать `version` в PUT |

API запускается раньше BFF и UI: BFF ходит в него, а UI ходит в BFF.

Проверка, что всё работает:

```bash
curl -i http://localhost:8080/accounts/acc-1/settings        # 404: настроек ещё нет
curl -i -X PUT http://localhost:8080/accounts/acc-1/settings \
     -H "Content-Type: application/json" \
     -d '{"floor_price": 1.5, "currency": "USD", "blocked_domains": ["bad.com"]}'
curl -i http://localhost:8080/accounts/acc-1/settings        # 200, "version": 0
curl http://localhost:8080/v3/api-docs                       # живая OpenAPI-спецификация
```

Посмотреть данные в базе: `docker compose exec postgres psql -U marketplace` в marketplace-infra
(полезные запросы — в README marketplace-infra).

## Тесты

Все тесты, кроме юнит-тестов, используют **Testcontainers**: на время тестов в Docker поднимается настоящий
PostgreSQL, поэтому нужен запущенный Docker. docker-compose для тестов не нужен.

```bash
mvn test      # только тесты
mvn verify    # тесты + перегенерация openapi.json
```

| Тест | Что проверяет |
|---|---|
| `SettingsApiTest` | API по HTTP (RestAssured): пути, статусы, JSON в snake_case, `version` |
| `SettingsServiceTest` | сервис с настоящей базой: версии, записи в outbox, «те же значения — нет новой версии» |
| `SettingsTransactionTest` | запись в outbox принудительно падает (мок репозитория) → изменение settings откатилось |
| `IdempotencyApiTest` | POST дважды с одним `Idempotency-Key` — домен добавлен один раз и ответ тот же; без ключа — дважды |
| `OptimisticLockingApiTest` | PUT с устаревшей `version` → 409; два параллельных PUT с одной `version` → один 200, другой 409 |
| `RollbackApiTest` | откат создаёт новую версию со старыми значениями и запись в outbox; 404 для неизвестной версии |
| `LostUpdateApiTest` | блокировка выключена: запись с устаревшей `version` проходит и затирает чужое изменение |
| `MigrationsTest` | Flyway без Spring: все миграции на пустой базе; данные, вставленные до V2, после неё на месте и с `version = 0` |

**Почему тесты сервиса теперь с базой, а не юнит.** Логика сервиса держится на поведении базы и Hibernate:
когда увеличивается `version`, что откатывается в транзакции. Юнит-тест с моками репозиториев проверял бы
поведение моков, а не это. Поэтому сервис тестируется с настоящим PostgreSQL.

**Почему RestAssured, а не MockMvc.** MockMvc вызывает контроллеры внутри того же процесса без
настоящего HTTP-сервера. RestAssured отправляет настоящие HTTP-запросы на запущенное приложение, как
это будет делать BFF. Для проекта про границы между сервисами нам важно проверять именно то, что видно
снаружи: пути, статусы, JSON в snake_case.

## OpenAPI-спецификация

`openapi.json` в корне **генерируется из кода**, руками его не правят.

```bash
mvn verify
```

Как это работает (см. `pom.xml`):
1. `spring-boot-maven-plugin` (goal `start`) поднимает приложение на порту 18080. Для этого используется
   `TestMarketplaceApiApplication` (в `src/test`): то же приложение, но PostgreSQL для него поднимает Testcontainers.
   Поэтому для генерации спецификации не нужен docker-compose, но нужен Docker;
2. `springdoc-openapi-maven-plugin` скачивает `http://localhost:18080/v3/api-docs` в `openapi.json`;
3. `spring-boot-maven-plugin` (goal `stop`) останавливает приложение.

После любого изменения контроллеров или модели запусти `mvn verify` и закоммить `openapi.json`
вместе с кодом. Иначе CI упадёт (см. ниже).

Модели: `SettingsValues` — тело запроса PUT, `Settings` — ответ (значения + `version`). Они разделены,
потому что `version` есть только в ответе.

Имена полей в JSON задаются аннотацией `@JsonProperty`. Глобальная настройка
`spring.jackson.property-naming-strategy` здесь не подходит: Spring Boot 4 сериализует JSON через
Jackson 3, а springdoc строит схему через Jackson 2. Глобальную стратегию видел бы только Spring, и
спецификация описывала бы `floorPrice`, хотя API отдаёт `floor_price`. Аннотацию `@JsonProperty`
читают обе версии Jackson.

## CI (`.github/workflows/ci.yml`)

| Job | Когда | Что проверяет |
|---|---|---|
| `test` | push в main, PR, вручную | `mvn verify` (тесты + генерация), затем `git diff --exit-code openapi.json` |
| `breaking-changes` | только PR | oasdiff сравнивает `openapi.json` ветки PR с `openapi.json` из main |

На раннерах GitHub Actions Docker уже есть, поэтому Testcontainers работает в CI без настройки.

## Инструменты: зачем каждый

- **Spring Boot** — веб-сервер, JSON, валидация, внедрение зависимостей. Без него пришлось бы писать много инфраструктурного кода.
- **Jakarta Validation (`@NotNull`, `@Valid`)** — возвращает 400 на неполный запрос и одновременно помечает поля как `required` в спецификации.
- **Spring Data JPA (Hibernate)** — отображает таблицы на Java-классы и генерирует SQL. `@Version` даёт счётчик версий,
  `@Transactional` — транзакции.
- **PostgreSQL** — настоящая база: транзакции, ограничения, типы `TEXT[]` и `JSONB`.
- **Flyway** — версионированные миграции схемы. Проблема, которую решает: у каждого разработчика, в CI и на
  сервере схема должна быть одинаковой и меняться одними и теми же шагами, а не руками.
- **Testcontainers** — поднимает настоящий PostgreSQL в Docker на время тестов. Проблема, которую решает:
  тесты на H2 или моках не видят поведения настоящей базы (типы, транзакции, SQL-диалект).
- **springdoc-openapi** — строит OpenAPI-спецификацию по контроллерам и моделям. Проблема, которую решает: спецификация, написанная руками, со временем перестаёт совпадать с кодом. Здесь её источник — сам код.
- **springdoc-openapi-maven-plugin** — сохраняет спецификацию в файл одной командой, чтобы её можно было коммитить, смотреть в diff и отдавать потребителям.
- **Проверка `git diff --exit-code openapi.json`** — ловит ситуацию «код поменяли, спецификацию не перегенерировали». Без неё потребители (BFF) получили бы устаревший контракт из main.
- **oasdiff** — знает правила совместимости OpenAPI: удалить поле ответа, сделать поле запроса обязательным или сменить тип — это breaking change, а добавить необязательное поле — нет. Проблема, которую решает: CI этого репозитория зелёный (тесты API проверяют только сам API), а потребитель при этом ломается. `fail-on: ERR` — падать только на точно ломающих изменениях (уровень WARN — «возможно ломающие»). `review: false` — не загружать спецификации на внешний сервис oasdiff.com.
- **JUnit 5 + AssertJ** — тесты. **RestAssured** — API-тесты (почему именно он — см. выше).

## Сознательные упрощения

- Записи в outbox никогда не удаляются и не архивируются. В реальной системе отправленные записи чистят,
  иначе таблица растёт бесконечно.
- `payload` в outbox — значения настроек целиком (снимок), без отдельной схемы и версии формата сообщения.
- Колонки `floor_price` и `currency` без ограничений (`NUMERIC` без точности, `VARCHAR(255)`): правил валидации
  в API пока нет.
- Версии считаются с 0: так работает `@Version` в Hibernate.
- Значения для отката берутся из outbox: там снимок каждой версии, и записи не удаляются. В реальной системе
  outbox чистят, а историю версий хранят в отдельной таблице.
- Ключи идемпотентности хранятся вечно. В реальной системе у них срок жизни (например, 24 часа) и их чистят.
- Повтор с тем же `Idempotency-Key`, но другим телом запроса возвращает сохранённый ответ. Реальные API
  (например, Stripe) сохраняют и тело запроса и в этом случае отвечают ошибкой.
- Два одновременных запроса с одним ключом оба не найдут его и оба начнут операцию. Второй получит 409 от
  `@Version` (оба меняют одни и те же настройки). Отдельной блокировки «ключ в обработке» нет.
- `version` в PUT необязательна, чтобы не ломать BFF и UI, которые её не передают. Для них защищает только
  `@Version` от одновременных записей, а не проверка «клиент видел устаревшие данные».

## Куда встроится то, что будет позже

- **Auth и tenancy** — аутентификация будет фильтром Spring Security перед контроллером, а проверка «пользователь имеет доступ к этому account_id» — в `SettingsService`.
- **Pact** — BFF опубликует контракт своих ожиданий, а этот репозиторий будет проверять его в CI (provider verification).

## Эксперименты

См. [EXPERIMENTS.md](https://github.com/DariaGuzich/marketplace-infra/blob/main/EXPERIMENTS.md) в marketplace-infra.
