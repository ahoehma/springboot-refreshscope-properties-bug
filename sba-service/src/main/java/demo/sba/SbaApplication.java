package demo.sba;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAdminServer
public class SbaApplication {

  public static void main(String[] args) {
    SpringApplication.run(SbaApplication.class, args);
  }
}
