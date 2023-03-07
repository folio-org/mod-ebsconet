package org.folio.ebsconet;

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.response.Response;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.TestSocketUtils;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.yml")
@Log4j2
public class TestBase {

  private static Header header;
  public static String TEST_TENANT = "test_tenant";

  @LocalServerPort
  protected int okapiPort;
  public final static int WIRE_MOCK_PORT = TestSocketUtils.findAvailableTcpPort();

  @Autowired
  private FolioModuleMetadata moduleMetadata;

  public static String getOkapiUrl() {
    return String.format("http://localhost:%s", WIRE_MOCK_PORT);
  }

  @BeforeAll
  static void testSetup() {
    header = new Header(XOkapiHeaders.TENANT, TEST_TENANT);
  }

  public <T> Response get(String url) {
    return given()
      .header(header)
      .contentType(ContentType.JSON)
      .get(url);
  }

  public Response put(String url, Object entity) {
    return given()
      .header(header)
      .contentType(ContentType.JSON)
      .body(entity)
      .put(url);
  }
}
