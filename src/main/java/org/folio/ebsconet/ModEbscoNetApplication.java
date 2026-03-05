package org.folio.ebsconet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class ModEbscoNetApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModEbscoNetApplication.class, args);
  }
}
