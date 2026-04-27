package demo.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

@SpringBootApplication
public class AppApplication {

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(AppApplication.class);
    // SAME setup as the production system: BufferingApplicationStartup with 2048 capacity
    application.setApplicationStartup(new BufferingApplicationStartup(2048));
    application.run(args);
  }
}
