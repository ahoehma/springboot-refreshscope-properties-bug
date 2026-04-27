package demo.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

@SpringBootApplication
public class AppApplication {

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(AppApplication.class);
    // SAME setup as the production system: BufferingApplicationStartup with 2048 capacity.
    // The proxy-serialization bug reproduces without this too — but with this set, the leaked
    // targetSource tree includes the entire events[] buffer, scaling the leak by orders of magnitude.
    application.setApplicationStartup(new BufferingApplicationStartup(2048));
    application.run(args);
  }
}
