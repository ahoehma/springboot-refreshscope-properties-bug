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

## What to look at

1. Open http://localhost:9090 — you should see `app-service` registered.
2. Click into the instance → **Configuration Properties**.
3. Look for the two beans:
   - `demo.with-refresh.*` — `@RefreshScope` bean
   - `demo.without-refresh.*` — control, no `@RefreshScope`
4. Compare their property trees. The expected bug:
   `demo.with-refresh` shows the legitimate fields (`enabled`, `maxSize`, `name`)
   PLUS spurious `targetSource.beanFactory.applicationStartup.bufferedTimeline.events[N].endTime`
   entries, often hundreds of them. `demo.without-refresh` shows only the legitimate fields.

## Direct curl

```
curl -s http://localhost:8080/actuator/configprops | jq '.contexts | to_entries[].value.beans | with_entries(select(.key | test("demo")))'
```

Look for the keys under `demo.with-refresh`. If they include `targetSource`, the bug is reproduced.

## Cleanup

Just kill both Maven windows. No docker, no DB, nothing persistent.
