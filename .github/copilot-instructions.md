## External Integrations
- Never hardcode credentials, bucket names, URLs, regions, or secrets
- Use Spring configuration properties for external integrations
- Prefer constructor injection for integration clients

## File Processing / Imports
- Process records defensively: invalid rows should not terminate the full job
- Continue processing valid rows when individual rows fail
- Validate required fields before processing
- Prefer streaming/iterative parsing for files rather than loading everything into memory

## Logging
- Log actionable errors with enough context for troubleshooting
- Never log secrets, credentials, or sensitive payload contents

## Quality Bar
- New functionality requires unit tests
- Include edge cases: empty input, malformed rows, invalid timestamps, missing resource
- All tests must pass before completion