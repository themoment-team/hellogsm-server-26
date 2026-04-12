# SDK Configuration — Reference

All SDK features are configured under the `sdk:` key in `application.yml`.

## Full Configuration Example

```yaml
sdk:
  logging:
    enabled: true
    not-logging-urls:
      - "/v3/api-docs/**"
      - "/swagger-ui/**"

  response:
    enabled: true
    not-wrapping-urls:
      - "/v3/api-docs/**"

  exception:
    enabled: true
    use-english-message: true   # uses English for internal error messages

  swagger:
    enabled: true
    title: "ReadyGSM API"
    paths-to-match:
      - "/v1/**"                # only document endpoints under /v1/
```

## Options

### `sdk.logging`
| Key | Type | Description |
|-----|------|-------------|
| `enabled` | boolean | Enable/disable automatic HTTP request/response logging |
| `not-logging-urls` | list | URL patterns to exclude from logging (Ant-style) |

### `sdk.response`
| Key | Type | Description |
|-----|------|-------------|
| `enabled` | boolean | Enable/disable automatic `CommonApiResponse` wrapping |
| `not-wrapping-urls` | list | URL patterns to exclude from wrapping (Ant-style) |

### `sdk.exception`
| Key | Type | Description |
|-----|------|-------------|
| `enabled` | boolean | Enable/disable `ExpectedException` → error response conversion |
| `use-english-message` | boolean | Use English for the default fallback error messages |

### `sdk.swagger`
| Key | Type | Description |
|-----|------|-------------|
| `enabled` | boolean | Enable/disable Swagger auto-configuration |
| `title` | string | API title shown in Swagger UI |
| `paths-to-match` | list | URL patterns to include in Swagger documentation |