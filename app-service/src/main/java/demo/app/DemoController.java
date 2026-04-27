package demo.app;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test rig for verifying whether {@code @RefreshScope} on
 * {@code @ConfigurationProperties} is actually necessary, or whether
 * Spring Cloud's {@code ConfigurationPropertiesRebinder} alone (which
 * fires on {@link EnvironmentChangeEvent}) updates the bean fields
 * even without {@code @RefreshScope}.
 *
 * <ul>
 *   <li>{@code GET  /demo/values} — current values of both beans + their identity hash</li>
 *   <li>{@code POST /demo/set?key=...&value=...} — push a key into a high-priority
 *       in-memory PropertySource, then publish an {@link EnvironmentChangeEvent}
 *       — exactly what the production system does on a runtime override apply.</li>
 * </ul>
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

  static final String SOURCE_NAME = "demo-runtime-overrides";

  private final WithRefreshScopeProperties withRefresh;
  private final WithoutRefreshScopeProperties withoutRefresh;
  private final ConfigurableEnvironment env;
  private final ApplicationEventPublisher publisher;

  public DemoController(WithRefreshScopeProperties withRefresh,
                        WithoutRefreshScopeProperties withoutRefresh,
                        ConfigurableEnvironment env,
                        ApplicationEventPublisher publisher) {
    this.withRefresh = withRefresh;
    this.withoutRefresh = withoutRefresh;
    this.env = env;
    this.publisher = publisher;
  }

  @GetMapping("/values")
  public Map<String, Object> values() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("withRefresh", snapshot(withRefresh.isEnabled(), withRefresh.getMaxSize(), withRefresh.getName(),
        System.identityHashCode(withRefresh), withRefresh.getClass().getName()));
    out.put("withoutRefresh", snapshot(withoutRefresh.isEnabled(), withoutRefresh.getMaxSize(), withoutRefresh.getName(),
        System.identityHashCode(withoutRefresh), withoutRefresh.getClass().getName()));
    return out;
  }

  @PostMapping("/set")
  public Map<String, Object> set(@RequestParam String key, @RequestParam String value) {
    pushIntoEnv(key, value);
    publisher.publishEvent(new EnvironmentChangeEvent(Set.of(key)));
    return Map.of("set", key + "=" + value, "publishedEvent", "EnvironmentChangeEvent");
  }

  private static Map<String, Object> snapshot(boolean enabled, int maxSize, String name, int hash, String className) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("enabled", enabled);
    m.put("maxSize", maxSize);
    m.put("name", name);
    m.put("identityHash", hash);
    m.put("class", className);
    return m;
  }

  private void pushIntoEnv(String key, String value) {
    PropertySource<?> existing = env.getPropertySources().get(SOURCE_NAME);
    if (existing instanceof MapPropertySource mps) {
      @SuppressWarnings("unchecked")
      Map<String, Object> backing = (Map<String, Object>) mps.getSource();
      backing.put(key, value);
    } else {
      Map<String, Object> map = new HashMap<>();
      map.put(key, value);
      env.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, map));
    }
  }
}
