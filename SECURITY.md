# Security policy

Please report vulnerabilities privately through GitHub's security advisory interface. Do not open a public issue containing credentials or exploit details.

## Credential policy

- No real credentials, connection strings, access tokens, source exports, or customer data belong in this repository.
- Local values go in `.env`, which Git ignores.
- Databricks values belong in the `schemaflow` secret scope.
- The public demo intentionally runs without external credentials.

The API does not expose arbitrary SQL execution or unrestricted document writes. If you adapt the project for production, add authentication, authorization, rate limiting, audit storage, TLS between every service, and organization-specific retention controls.
