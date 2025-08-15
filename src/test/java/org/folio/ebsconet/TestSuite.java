package org.folio.ebsconet;

import org.folio.ebsconet.controller.OrdersControllerTest;
import org.folio.ebsconet.service.NotesServiceTest;
import org.folio.ebsconet.service.OrdersServiceTest;
import org.junit.jupiter.api.Nested;

public class TestSuite {

  @Nested
  class OrdersServiceTestNested extends OrdersServiceTest {
  }

  @Nested
  class OrdersControllerTestNested extends OrdersControllerTest {
  }

  @Nested
  class NotesServiceTestNested extends NotesServiceTest {
  }
}
