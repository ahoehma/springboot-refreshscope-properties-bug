# Spring Boot 4 + `@RefreshScope` `@ConfigurationProperties` + `/actuator/configprops` repro

Reproduces a `/actuator/configprops` JSON pollution bug suspected in
Spring Boot 4.0.6 / Spring Cloud 2025.1.1: a `@RefreshScope`-annotated
`@ConfigurationProperties` bean leaks its CGLIB AOP-proxy internals
(`targetSource → beanFactory → applicationStartup → bufferedTimeline.events[…]`)
into the actuator response when `BufferingApplicationStartup` is configured.

## Versions
- Spring Boot **4.0.6**
- Spring Cloud **2025.1.1**
- Spring Boot Admin **4.0.4**
- Java **25**

## Layout
```
app-service/   port 8080  — has WithRefreshScope + WithoutRefreshScope properties beans, BufferingApplicationStartup(2048)
sba-service/   port 9090  — Spring Boot Admin server, no security
```

No Spring Security is included anywhere. Both services are wide-open.

## How to start

### Windows
Double-click or run from a CMD shell:
```
start.cmd
```
Two windows open. Wait ~10s.

### Manual (any OS)
Two terminals:
```
mvn -pl sba-service spring-boot:run
mvn -pl app-service spring-boot:run
```

## What to look at — Bug 1: `/actuator/configprops` proxy leak

1. Open http://localhost:9090 — you should see `app-service` registered.
2. Click into the instance → **Configuration Properties**.
3. Look for the two beans:
   - `demo.with-refresh.*` — `@RefreshScope` bean
   - `demo.without-refresh.*` — control, no `@RefreshScope`
4. Compare their property trees. The expected bug:
   `demo.with-refresh` shows the legitimate fields (`enabled`, `maxSize`, `name`)
   PLUS spurious `targetSource.beanFactory.applicationStartup.bufferedTimeline.events[N].endTime`
   entries, often hundreds of them. `demo.without-refresh` shows only the legitimate fields.

Direct curl:
```
curl -s http://localhost:8080/actuator/configprops \
  | jq '.contexts | to_entries[].value.beans | with_entries(select(.key | test("demo")))'
```

## What to look at — Bug 2: `@RefreshScope` blocks `ConfigurationPropertiesRebinder`

Even more interesting: an `EnvironmentChangeEvent` (the trigger
`ConfigurationPropertiesRebinder` reacts to) silently fails to rebind
the `@RefreshScope`-wrapped bean. `DemoController` exposes the proof:

```bash
# 1. read initial values
curl -s http://localhost:8080/demo/values

# 2. push a new value into the env + publish EnvironmentChangeEvent
curl -X POST "http://localhost:8080/demo/set?key=demo.with-refresh.max-size&value=999"
curl -X POST "http://localhost:8080/demo/set?key=demo.without-refresh.max-size&value=999"

# 3. read again — withoutRefresh.maxSize == 999, withRefresh.maxSize STILL == 200
curl -s http://localhost:8080/demo/values

# 4. trigger /actuator/refresh (clears RefreshScope) and read once more —
#    now withRefresh.maxSize == 999 too
curl -X POST http://localhost:8080/actuator/refresh
curl -s http://localhost:8080/demo/values
```

The takeaway: **`@RefreshScope` on a `@ConfigurationProperties` bean is
an anti-pattern.** `ConfigurationPropertiesRebinder` already handles
re-binding on `EnvironmentChangeEvent` for plain `@ConfigurationProperties`
beans. Adding `@RefreshScope` hides the bean behind a CGLIB proxy whose
scoped target is invisible to the rebinder, so live refresh requires the
much heavier `/actuator/refresh` call instead.

## Cleanup

Just kill both Maven windows. No docker, no DB, nothing persistent.
