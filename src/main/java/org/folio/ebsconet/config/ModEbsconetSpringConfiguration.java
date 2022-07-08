package org.folio.ebsconet.config;

import com.fasterxml.jackson.databind.Module;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.AbstractRequestLoggingFilter;
import javax.servlet.http.HttpServletRequest;

@Configuration
public class ModEbsconetSpringConfiguration {
  @Bean
  public Module jsonNullableModule() {
    return new JsonNullableModule();
  }

  @Bean
  public InfoRequestsLoggingFilter requestLoggingFilter() {
    InfoRequestsLoggingFilter loggingFilter = new InfoRequestsLoggingFilter();
    loggingFilter.setIncludeClientInfo(true);
    loggingFilter.setIncludeQueryString(true);
    loggingFilter.setIncludePayload(true);
    loggingFilter.setMaxPayloadLength(128000);
    return loggingFilter;
  }

  private static class InfoRequestsLoggingFilter extends AbstractRequestLoggingFilter {

    @Override
    protected boolean shouldLog(HttpServletRequest request) {
      return logger.isInfoEnabled();
    }

    @Override
    protected void beforeRequest(HttpServletRequest request, String message) {
      logger.info(message);
    }

    @Override
    protected void afterRequest(HttpServletRequest request, String message) {
      logger.info(message);
    }
  }
}
