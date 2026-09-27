# F1 Strategy Simulator

Explore Formula 1 race strategy by building tyre stints, simulating their predicted race time, and comparing predictions with real results. The project pairs an Angular interface with a Spring Boot API and uses race data from [OpenF1](https://openf1.org/).

## Contents

- [Quick start](#quick-start)
- [Using the simulator](#using-the-simulator)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License and project status](#license-and-project-status)

## Quick start

**Requirements:** Docker with Compose, Node.js, and npm.

1. Start PostgreSQL, Redis, and the API from the repository root:

   ```sh
   docker compose up --build -d
   ```

   The API runs at `http://localhost:8090`. Database migrations are applied at startup.

2. In a second terminal, install and start the Angular app:

   ```sh
   cd frontend
   npm ci
   npm start
   ```

3. Open [http://localhost:4210](http://localhost:4210), then register or sign in to build a strategy.

The race list is populated from the backend database. For a fresh database, sync a season from OpenF1 after signing in. The sync endpoint is authenticated; in the browser developer console, copy the value of `localStorage.getItem('f1sim_token')`, then run the following from PowerShell or a terminal, replacing `<JWT>` with that value:

```sh
curl.exe -X POST "http://localhost:8090/api/admin/sync/season/2024" -H "Authorization: Bearer <JWT>"
```

Change `2024` to the season you want to import. OpenF1 availability and request limits apply.

## Using the simulator

1. Choose a circuit on the home screen and select a listed race driver.
2. Register or sign in, then open **Build a strategy**.
3. Add or remove stints, choose a tyre compound, and set the lap range for each stint. The plan must cover the race distance.
4. Run the simulation to see its predicted total time. For a finished race with available results, the app also shows the difference from the actual result.
5. Visit **Leaderboard** to compare the most accurate scored prediction per user and circuit. A profile page is available for signed-in users.

The available compounds are Soft, Medium, Hard, Intermediate, and Wet. Race weather windows are shown in the strategy builder when data is available. Comparisons are estimates: the model does not account for safety cars, red flags, or other race incidents.

<details>
<summary>More about the simulation model</summary>

The backend calculates each stint lap from a base lap time, compound pace delta, linearly increasing tyre degradation, and a wet/dry compound mismatch penalty. Each pit stop adds the circuit's pit-lane time loss and the driver's team's average pit-stop time. The simplified model is implemented in [`StrategyEngineService`](backend/src/main/java/com/f1sim/service/StrategyEngineService.java).

</details>

## Configuration

`docker-compose.yml` supplies local PostgreSQL and Redis settings to the backend. The Spring configuration also supports these environment variables:

| Variable | Purpose | Default |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5435/f1sim` |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials | `f1sim` / `f1sim` |
| `REDIS_HOST` / `REDIS_PORT` | Redis connection | `localhost` / `6381` |
| `JWT_SECRET` | Signing key for authentication tokens | Development-only placeholder |

Set a strong `JWT_SECRET` and non-development database credentials before exposing the service beyond a local environment. The OpenF1 base URL is configured in [`application.yml`](backend/src/main/resources/application.yml).

<details>
<summary>Ports and service commands</summary>

| Service | Local address | Compose service |
| --- | --- | --- |
| Frontend | `http://localhost:4210` | Run separately with `npm start` |
| Backend API | `http://localhost:8090` | `backend` |
| PostgreSQL | `localhost:5435` | `db` |
| Redis | `localhost:6381` | `redis` |

The Angular development server proxies `/api` requests to the backend. To stop the Compose services, run `docker compose down` from the repository root. The named `f1sim-db-data` volume keeps database data when containers stop.

</details>

## Architecture

- **Frontend:** Angular 18 standalone components, lazy-loaded routes, and API services live under [`frontend/src/app`](frontend/src/app/). Circuit outlines and the circuit-to-race mapping are local frontend data.
- **Backend:** Spring Boot 3 REST controllers and services live under [`backend/src/main/java/com/f1sim`](backend/src/main/java/com/f1sim/). Authentication uses JWT; race browsing is public while strategy simulation requires sign-in.
- **Persistence and ingestion:** PostgreSQL entities and repositories are managed with Flyway migrations in [`backend/src/main/resources/db/migration`](backend/src/main/resources/db/migration/). [`RaceSyncService`](backend/src/main/java/com/f1sim/service/RaceSyncService.java) imports meetings, sessions, drivers, incidents, and weather from OpenF1.
- **API reference:** When the backend is running, open [Swagger UI](http://localhost:8090/swagger-ui.html).

The backend build and Java version are defined in [`backend/pom.xml`](backend/pom.xml); frontend scripts are in [`frontend/package.json`](frontend/package.json). Local service definitions are in [`docker-compose.yml`](docker-compose.yml).

## Troubleshooting

<details>
<summary>The race list is empty</summary>

A new database has no synced races. Sign in, then use the authenticated OpenF1 season-sync request in [Quick start](#quick-start). Confirm the selected season has data available from OpenF1.

</details>

<details>
<summary>The frontend cannot reach the API</summary>

Check that the Compose backend is healthy and listening on port `8090`, and that the frontend is running on `4210` (the configured API proxy target). Inspect backend output with `docker compose logs backend`.

</details>

<details>
<summary>Database connection or schema errors</summary>

Start Compose from the repository root and check `docker compose logs db backend`. The backend applies Flyway migrations during startup; confirm PostgreSQL is ready and the configured database credentials match.

</details>

## Contributing

Bug reports and focused pull requests are welcome. For significant changes, open an issue first to discuss the proposed behavior. Include the steps or tests used to verify a change; keep frontend and backend changes consistent where they share API behavior.

## License and project status

No `LICENSE` file is currently present, so the repository does not specify usage or distribution permissions. The project currently provides race browsing, account registration and login, strategy simulation, and circuit leaderboards; a hosted deployment is not documented.
