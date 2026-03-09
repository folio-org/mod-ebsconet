package org.folio.ebsconet.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.cfg.DateTimeFeature;

@Configuration
public class ModEbsconetSpringConfiguration {

  @Bean
  public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
    return builder -> builder.configure(DateTimeFeature.WRITE_UTC_AS_OFFSET, true);
  }

}
