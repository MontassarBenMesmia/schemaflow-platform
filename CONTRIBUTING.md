# Contributing

1. Create a focused branch from `main`.
2. Never use real personal, customer, internship, or production data.
3. Run `mvn verify` in `backend`, then `npm test -- --watch=false` and `npm run build` in `frontend`.
4. Validate `docker compose config --quiet`.
5. Explain migration-contract or security changes in the pull request.

Commits should be small, descriptive, and free of generated output.
