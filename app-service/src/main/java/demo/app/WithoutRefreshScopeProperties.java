package demo.app;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Control: same shape but WITHOUT @RefreshScope.
 * Should serialize cleanly via /actuator/configprops.
 */
@Component
@ConfigurationProperties(prefix = "demo.without-refresh")
public class WithoutRefreshScopeProperties {

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
