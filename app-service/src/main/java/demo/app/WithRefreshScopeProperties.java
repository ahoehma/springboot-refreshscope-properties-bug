package demo.app;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Reproduces the suspected bug: a @RefreshScope @ConfigurationProperties bean
 * gets serialized via /actuator/configprops and the AOP proxy's Advised methods
 * leak the entire BeanFactory + ApplicationStartup buffer into the JSON response.
 */
@Component
@ConfigurationProperties(prefix = "demo.with-refresh")
@RefreshScope
public class WithRefreshScopeProperties {

  private boolean enabled = true;
  private int maxSize = 100;
  private String name = "default";

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }

  public int getMaxSize() { return maxSize; }
  public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
}
