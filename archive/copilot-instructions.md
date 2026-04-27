# Copilot Instructions

## Package & File Conventions
- Packages: `controller`, `service`, `repository`, `model`, `dto`, `exception` under `com.example.hellocopilot`
- DTOs: `XxxRequest` / `XxxResponse` in `dto/`, built with Lombok `@Builder`
- Services: always a `XxxService` interface + `XxxServiceImpl` in the same `service/` package

## Registering New Modules
Adding a new entity requires all of: `@Entity` model, `JpaRepository` repository, service interface + impl, `@RestController`, **and** a Flyway migration file.
- Schema changes go in `src/main/resources/db/migration/V{n}__description.sql` — `ddl-auto: validate` will fail at startup otherwise.
- New custom exceptions must get a matching `@ExceptionHandler` in `GlobalExceptionHandler`.

## Service Layer Pattern
- Impl class: `@Service @Transactional @RequiredArgsConstructor @Slf4j`
- Read-only methods must add `@Transactional(readOnly = true)`
- Inject dependencies via constructor (Lombok `@RequiredArgsConstructor`), never `@Autowired`
- Map entity → response using a private `toResponse()` method inside the impl

## API Layer Pattern
- All endpoints under `/api/xxx`; controllers return `ResponseEntity<XxxResponse>`
- Request body validation via `@Valid`; validation errors are handled globally — do not catch `MethodArgumentNotValidException` in controllers

## Testing
- **Controllers**: `@WebMvcTest(controllers = { XxxController.class, GlobalExceptionHandler.class })` — must explicitly list `GlobalExceptionHandler` or exception-mapping tests will fail
- **Services**: `@ExtendWith(MockitoExtension.class)` with `@Mock` / `@InjectMocks`
- **Repositories**: `@DataJpaTest` (uses H2 + Flyway automatically)
- Assertions use AssertJ (`assertThat`); every test needs `@DisplayName`