package org.folio.ebsconet.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.ebsconet.client.FinanceClient;
import org.folio.ebsconet.client.OrdersClient;
import org.folio.ebsconet.client.OrganizationClient;
import org.folio.ebsconet.domain.dto.Cost;
import org.folio.ebsconet.domain.dto.Details;
import org.folio.ebsconet.domain.dto.EbsconetOrderLine;
import org.folio.ebsconet.domain.dto.ExpenseClass;
import org.folio.ebsconet.domain.dto.ExpenseClassCollection;
import org.folio.ebsconet.domain.dto.Fund;
import org.folio.ebsconet.domain.dto.FundCollection;
import org.folio.ebsconet.domain.dto.FundDistribution;
import org.folio.ebsconet.domain.dto.Location;
import org.folio.ebsconet.domain.dto.OrderFormat;
import org.folio.ebsconet.domain.dto.Organization;
import org.folio.ebsconet.domain.dto.PaymentStatus;
import org.folio.ebsconet.domain.dto.PoLine;
import org.folio.ebsconet.domain.dto.PoLineCollection;
import org.folio.ebsconet.domain.dto.PurchaseOrder;
import org.folio.ebsconet.domain.dto.ReceiptStatus;
import org.folio.ebsconet.domain.dto.VendorDetail;
import org.folio.ebsconet.domain.dto.WorkflowStatus;
import org.folio.ebsconet.error.ResourceNotFoundException;
import org.folio.ebsconet.error.UnprocessableEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;

import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import feign.Request;
import feign.Request.Body;
import feign.Request.HttpMethod;
import feign.RequestTemplate;

@ExtendWith(MockitoExtension.class)
public class OrdersServiceTest {

  @Mock
  private OrdersClient ordersClient;
  @Mock
  private FinanceClient financeClient;
  @Mock
  private OrganizationClient organizationClient;
  @Mock
  private NotesService notesService;
  @InjectMocks
  private OrdersService ordersService;

  private PoLine preparePoLine(String poLineNumber) throws IOException, URISyntaxException {
    Path path = Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource("mockdata/order_line.json")).toURI());
    try (Stream<String> lines = Files.lines(path)) {
      String data = lines.collect(Collectors.joining("\n"));
      PoLine pol = new ObjectMapper().readValue(data, PoLine.class);
      pol.setPoLineNumber(poLineNumber);
      return pol;
    }
  }

  private Organization prepareOrganization(String vendorId) {
    var vendorOrg = new Organization();
    vendorOrg.setId(vendorId);
    vendorOrg.setCode("AMAZ");
    return vendorOrg;
  }

  private PurchaseOrder preparePurchaseOrder(String poId, String vendorId) {
    var po = new PurchaseOrder();
    po.setId(poId);
    po.setNotes(List.of("Notes in the purchase order"));
    po.setVendor(vendorId);
    po.setWorkflowStatus(WorkflowStatus.OPEN);
    return po;
  }

  @Test
  void testGetEbsconetOrderLine() throws IOException, URISyntaxException {
    String poLineNumber = "268758-03";
    PoLine pol = preparePoLine(poLineNumber);
    pol.getFundDistribution().getFirst().setExpenseClassId(UUID.randomUUID().toString());
    var polResult = new PoLineCollection();
    polResult.addPoLinesItem(pol);
    polResult.setTotalRecords(1);

    String vendorId = "168f8a86-d26c-406e-813f-c7527f241ac3";
    Organization vendorOrg = prepareOrganization(vendorId);

    String poId = "d79b0bcc-DcAD-1E4E-Abb7-DbFcaD5BB3bb";
    PurchaseOrder po = preparePurchaseOrder(poId, vendorId);

    ExpenseClass expClass = new ExpenseClass().id(UUID.randomUUID().toString()).code("Prnt");

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);
    when(ordersClient.getOrderById(poId)).thenReturn(po);
    when(organizationClient.getOrganizationById(vendorId)).thenReturn(vendorOrg);
    when(financeClient.getExpenseClassesById(anyString())).thenReturn(expClass);

    EbsconetOrderLine ebsconetOL = ordersService.getEbscoNetOrderLine(poLineNumber);

    verify(ordersClient, times(1)).getOrderLinesByQuery(isA(String.class));
    verify(ordersClient, times(1)).getOrderById(isA(String.class));
    verify(organizationClient, times(1)).getOrganizationById(isA(String.class));

    assertThat(ebsconetOL.getFundCode(), is("HIST:Prnt"));
    assertThat(ebsconetOL.getSubscriptionFromDate(), is(JsonNullable.undefined()));
    // NOTE: a more complete value check is done with the controller test
  }

  @Test
  void testGetEbsconetOrderLine404() {
    String poLineNumber = "268758-03";

    var polResult = new PoLineCollection();
    polResult.setTotalRecords(0);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);

    assertThrows(ResourceNotFoundException.class, () -> ordersService.getEbscoNetOrderLine(poLineNumber));
  }

  @Test
  void testGetEbsconetOrderLineWithNoFundDistributionCode() throws IOException, URISyntaxException {
    String poLineNumber = "268758-03";
    PoLine pol = preparePoLine(poLineNumber);
    pol.setFundDistribution(null);

    var polResult = new PoLineCollection();
    polResult.addPoLinesItem(pol);
    polResult.setTotalRecords(1);

    String vendorId = "168f8a86-d26c-406e-813f-c7527f241ac3";
    Organization vendorOrg = prepareOrganization(vendorId);

    String poId = "d79b0bcc-DcAD-1E4E-Abb7-DbFcaD5BB3bb";
    PurchaseOrder po = preparePurchaseOrder(poId, vendorId);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);
    when(ordersClient.getOrderById(poId)).thenReturn(po);
    when(organizationClient.getOrganizationById(vendorId)).thenReturn(vendorOrg);

    EbsconetOrderLine ebsconetOL = ordersService.getEbscoNetOrderLine(poLineNumber);

    assertThat(ebsconetOL.getFundCode(), nullValue());
  }

  @Test
  void shouldCallPutIfCallUpdateEbsconetOrderLineWithElectronic() {
    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("CODE", 1);

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    var poLine2 = new PoLine();
    var fundDistribution = new FundDistribution();
    fundDistribution.setCode("CODE");
    poLine2.setId(poLine1.getId());
    poLine2.setFundDistribution(Collections.singletonList(fundDistribution));
    poLine2.setCost(new Cost());
    poLine2.setVendorDetail(new VendorDetail());
    poLine2.setDetails(new Details());
    poLine2.setLocations(Collections.singletonList(new Location()));
    poLine2.setOrderFormat(OrderFormat.ELECTRONIC_RESOURCE);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);
    when(ordersClient.getOrderLineById("id")).thenReturn(poLine2);

    FundCollection fundCollection = new FundCollection()
      .funds(Collections.singletonList(new Fund()))
      .totalRecords(1);
    when(financeClient.getFundsByQuery("code==CODE")).thenReturn(fundCollection);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    verify(ordersClient, times(1)).getOrderLinesByQuery(anyString());
    verify(ordersClient, times(1)).getOrderLineById(anyString());
    verify(ordersClient, times(1)).putOrderLine(anyString(), any());
  }

  @Test
  void shouldCallPutOrderLineWithElectronicWithoutVendorDetail() {
    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("CODE", 1);

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    var poLine2 = new PoLine();
    var fundDistribution = new FundDistribution();
    fundDistribution.setCode("CODE");
    poLine2.setId(poLine1.getId());
    poLine2.setFundDistribution(Collections.singletonList(fundDistribution));
    poLine2.setCost(new Cost());
    poLine2.setDetails(new Details());
    poLine2.setLocations(Collections.singletonList(new Location()));
    poLine2.setOrderFormat(OrderFormat.ELECTRONIC_RESOURCE);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);
    when(ordersClient.getOrderLineById("id")).thenReturn(poLine2);

    FundCollection fundCollection = new FundCollection()
      .funds(Collections.singletonList(new Fund()))
      .totalRecords(1);
    when(financeClient.getFundsByQuery("code==CODE")).thenReturn(fundCollection);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    verify(ordersClient, times(1)).getOrderLinesByQuery(anyString());
    verify(ordersClient, times(1)).getOrderLineById(anyString());
    verify(ordersClient, times(1)).putOrderLine(anyString(), any());
  }

  @Test
  void shouldCallPutIfCallUpdateEbsconetOrderLine() {
    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("CODE", 1);

    var testRenewalNote = "Test renewal Note";
    ebsconetOrderLine.internalNote(testRenewalNote);
    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    var poLine2 = new PoLine();
    var fundDistribution = new FundDistribution();
    fundDistribution.setCode("CODE");
    poLine2.setId(poLine1.getId());
    poLine2.setFundDistribution(Collections.singletonList(fundDistribution));
    poLine2.setCost(new Cost());
    poLine2.setVendorDetail(new VendorDetail());
    poLine2.setDetails(new Details());
    poLine2.setLocations(Collections.singletonList(new Location()));
    poLine2.setOrderFormat(OrderFormat.PHYSICAL_RESOURCE);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);
    when(ordersClient.getOrderLineById("id")).thenReturn(poLine2);

    FundCollection fundCollection = new FundCollection()
      .funds(Collections.singletonList(new Fund()))
      .totalRecords(1);
    when(financeClient.getFundsByQuery(anyString())).thenReturn(fundCollection);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    verify(ordersClient, times(1)).getOrderLinesByQuery(anyString());
    verify(ordersClient, times(1)).getOrderLineById(anyString());
    verify(ordersClient, times(1)).putOrderLine(anyString(), any());

    ArgumentCaptor<PoLine> argumentCaptor = ArgumentCaptor.forClass(PoLine.class);
    verify(ordersClient).putOrderLine(anyString(), argumentCaptor.capture());
    PoLine updatedLine = argumentCaptor.getValue();
    assertEquals(testRenewalNote, updatedLine.getRenewalNote());
  }

  @Test
  void shouldCallPutIfCallUpdateEbsconetOrderLineWithDifferentFundCode() {
    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("DIFFERENT_CODE", 1);

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId(UUID.randomUUID().toString());
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    var poLine2 = new PoLine();
    var fundDistribution = new FundDistribution();
    fundDistribution.setCode("CODE");
    poLine2.setId(poLine1.getId());
    poLine2.setFundDistribution(Collections.singletonList(fundDistribution));
    poLine2.setCost(new Cost());
    poLine2.setVendorDetail(new VendorDetail());
    poLine2.setDetails(new Details());
    poLine2.setLocations(Collections.singletonList(new Location()));
    poLine2.setOrderFormat(OrderFormat.PHYSICAL_RESOURCE);

    var funds = new FundCollection();
    var fund = new Fund();
    fund.setCode("DIFFERENT_CODE");
    fund.setId("id");
    funds.setFunds(Collections.singletonList(fund));
    funds.setTotalRecords(1);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);
    when(ordersClient.getOrderLineById(poLine1.getId())).thenReturn(poLine2);
    when(financeClient.getFundsByQuery(any())).thenReturn(funds);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    verify(ordersClient, times(1)).getOrderLinesByQuery(anyString());
    verify(ordersClient, times(1)).getOrderLineById(anyString());
    verify(ordersClient, times(1)).putOrderLine(anyString(), any());
    verify(financeClient, times(1)).getFundsByQuery(any());
  }

  @Test
  void shouldThrowExceptionIfPoLineNotFound() {
    var poline = new EbsconetOrderLine();
    poline.setPoLineNumber("1");

    Request request = Request.create(HttpMethod.GET, "", new HashMap<>(), Body.empty(), new RequestTemplate());
    when(ordersClient.getOrderLinesByQuery(any())).thenThrow(new FeignException.NotFound("", request, "".getBytes(), request.headers()));
    ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class,
      () -> ordersService.updateEbscoNetOrderLine(poline));
    assertThat(resourceNotFoundException.getMessage(), is("PO Line not found: 1"));
  }

  @Test
  void shouldThrowExceptionIfPoLineRecordsLessThenOne() {
    var ebsconetOrderLine = new EbsconetOrderLine();
    ebsconetOrderLine.setPoLineNumber("1");
    var lines = new PoLineCollection();
    lines.setTotalRecords(0);

    when(ordersClient.getOrderLinesByQuery(any())).thenReturn(lines);

    ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class,
      () -> ordersService.updateEbscoNetOrderLine(ebsconetOrderLine));
    assertThat(resourceNotFoundException.getMessage(), is("PO Line not found: 1"));
  }

  @ParameterizedTest
  @CsvSource({
    "7, 1, 3, 1 ,6", // Update quantity where P=1, E>1
    "9, 4, 7, 5 ,4", // Update quantity where P>1, E>1
    "9, 1, 1, 5 ,4", // Update quantity where P=1, E=1
    "7, 3, 1, 6 ,1", // Update quantity where P>1, E=1
    // see https://issues.folio.org/browse/MODEBSNET-10
  })
  @DisplayName("Update P/E Mix line with new quantity")
  void updatePEMixLineWithNewQuantity(int ebsconetQuantity, int currentPQuantity, int currentEQuantity, int expectedPQuantity,
                                      int expectedEQuantity) {
    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("CODE", ebsconetQuantity);

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    PoLine poLine2 = getSampleCompPoLine();
    poLine2.getCost().setQuantityPhysical(currentPQuantity);
    poLine2.getCost().setQuantityElectronic(currentEQuantity);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);

    when(ordersClient.getOrderLineById("id")).thenReturn(poLine2);

    FundCollection fundCollection = new FundCollection()
      .funds(Collections.singletonList(new Fund()))
      .totalRecords(1);
    when(financeClient.getFundsByQuery(anyString())).thenReturn(fundCollection);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    ArgumentCaptor<PoLine> argumentCaptor = ArgumentCaptor.forClass(PoLine.class);
    verify(ordersClient).putOrderLine(any(), argumentCaptor.capture());
    var updatedCompLine = argumentCaptor.getValue();

    assertEquals(expectedEQuantity, updatedCompLine.getLocations().getFirst().getQuantityElectronic());
    assertEquals("percentage", updatedCompLine.getFundDistribution().getFirst().getDistributionType().getValue());
    assertEquals(expectedPQuantity, updatedCompLine.getLocations().getFirst().getQuantityPhysical());

    assertEquals(expectedEQuantity, updatedCompLine.getCost().getQuantityElectronic());
    assertEquals(expectedPQuantity, updatedCompLine.getCost().getQuantityPhysical());
  }

  @ParameterizedTest
  @DisplayName("Update P/E Mix line with new price")
  @MethodSource("getPriceParameters")
  void updatePEMixLineWithNewPrice(BigDecimal ebscoPrice, BigDecimal currentPPrice, BigDecimal currentEPrice,
                                   BigDecimal expectedPPrice, BigDecimal expectedEPrice) {
    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    // see https://issues.folio.org/browse/MODEBSNET-10
    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("CODE", 7);
    ebsconetOrderLine.setUnitPrice(ebscoPrice);
    PoLine poLine2 = getSampleCompPoLine();
    poLine2.getCost().setListUnitPrice(currentPPrice);
    poLine2.getCost().setListUnitPriceElectronic(currentEPrice);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);

    when(ordersClient.getOrderLineById("id")).thenReturn(poLine2);
    FundCollection fundCollection = new FundCollection()
      .funds(Collections.singletonList(new Fund()))
      .totalRecords(1);
    when(financeClient.getFundsByQuery(anyString())).thenReturn(fundCollection);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    ArgumentCaptor<PoLine> argumentCaptor = ArgumentCaptor.forClass(PoLine.class);
    verify(ordersClient).putOrderLine(any(), argumentCaptor.capture());
    var updatedCompLine = argumentCaptor.getValue();

    assertNotNull(updatedCompLine.getCost().getListUnitPriceElectronic());
    assertNotNull(updatedCompLine.getCost().getListUnitPrice());
    assertEquals(expectedEPrice.doubleValue(), updatedCompLine.getCost().getListUnitPriceElectronic().doubleValue(), 2);
    assertEquals(expectedPPrice.doubleValue(), updatedCompLine.getCost().getListUnitPrice().doubleValue(), 2);
    assertEquals("percentage", updatedCompLine.getFundDistribution().getFirst().getDistributionType().getValue());
  }

  @ParameterizedTest
  @CsvSource({
    "1, 1, 1, 1, 1",
    "2, 1, 1, 1, 1",
    "7, 3, 2, 4, 3",
  })
  @DisplayName("Update line with multiple locations. P/E Mix")
  void updateLineWithMultipleLocationsMix(int ebsconetQuantity,
                                          int currentPQuantity, int currentEQuantity, int newLocation1Quantity, int newLocation2Quantity) {
    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("CODE", ebsconetQuantity);

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    PoLine poLine2 = getSampleCompPoLine();

    List<Location> locations = new ArrayList<>();
    locations.add(new Location().locationId(UUID.randomUUID().toString()).quantityPhysical(currentPQuantity));
    locations.add(new Location().locationId(UUID.randomUUID().toString()).quantityElectronic(currentEQuantity));
    poLine2.setLocations(locations);

    poLine2.setOrderFormat(OrderFormat.P_E_MIX);

    poLine2.getCost().setQuantityPhysical(currentPQuantity);
    poLine2.getCost().setQuantityElectronic(currentEQuantity);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);

    when(ordersClient.getOrderLineById("id")).thenReturn(poLine2);

    FundCollection fundCollection = new FundCollection()
      .funds(Collections.singletonList(new Fund()))
      .totalRecords(1);
    when(financeClient.getFundsByQuery(anyString())).thenReturn(fundCollection);
    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    ArgumentCaptor<PoLine> argumentCaptor = ArgumentCaptor.forClass(PoLine.class);
    verify(ordersClient).putOrderLine(any(), argumentCaptor.capture());
    var updatedCompLine = argumentCaptor.getValue();

    assertEquals(newLocation1Quantity, updatedCompLine.getLocations().get(0).getQuantityPhysical());

    // check special case when PE mix splits 1 item into 1 physical + 1 electronic
    if (ebsconetQuantity == 1) {
      assertEquals(newLocation2Quantity, updatedCompLine.getLocations().get(1).getQuantityElectronic());
    } else {
      assertEquals(newLocation2Quantity, updatedCompLine.getLocations().getFirst().getQuantityElectronic());
    }
  }

  @ParameterizedTest
  @CsvSource(
    {
      "7, 3, 2,  Electronic Resource",
      "1, 3, 2,  Electronic Resource",
      "7, 3, 2,  Physical Resource",
      "1, 3, 2,  Physical Resource",
      "7, 3, 2,  P/E Mix",
      "1, 3, 2,  P/E Mix"
    })
  @DisplayName("Update line with emtpy locations (createInventory = NONE)")
  void updateLineWithEmptyLocationsMix(int ebsconetQuantity, int currentPQuantity, int currentEQuantity, String orderType) {
    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("CODE", ebsconetQuantity);

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    PoLine poLine2 = getSampleCompPoLine();

    poLine2.setLocations(new ArrayList<>());

    poLine2.setOrderFormat(OrderFormat.fromValue(orderType));

    poLine2.getCost().setQuantityPhysical(currentPQuantity);
    poLine2.getCost().setQuantityElectronic(currentEQuantity);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);

    when(ordersClient.getOrderLineById("id")).thenReturn(poLine2);

    FundCollection fundCollection = new FundCollection().funds(Collections.singletonList(new Fund())).totalRecords(1);
    when(financeClient.getFundsByQuery(anyString())).thenReturn(fundCollection);
    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    ArgumentCaptor<PoLine> argumentCaptor = ArgumentCaptor.forClass(PoLine.class);
    verify(ordersClient).putOrderLine(any(), argumentCaptor.capture());
    var updatedCompLine = argumentCaptor.getValue();

    assertTrue(CollectionUtils.isEmpty(updatedCompLine.getLocations()));
  }

  @ParameterizedTest
  @CsvSource({
    "1, 1, 1, 0",
    "2, 1, 2, 0",
    // see https://issues.folio.org/browse/MODEBSNET-18
  })
  @DisplayName("Update line with multiple locations. Physical")
  void updateLineWithMultipleLocationsPhysical(int ebsconetQuantity, int currentPQuantity, int newLocationQuantity) {
    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("CODE", ebsconetQuantity);

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    PoLine poLine2 = getSampleCompPoLine();

    List<Location> locations = new ArrayList<>();
    locations.add(new Location().locationId(UUID.randomUUID().toString()).quantityPhysical(currentPQuantity));
    locations.add(new Location().locationId(UUID.randomUUID().toString()).quantityPhysical(currentPQuantity));
    poLine2.setLocations(locations);

    poLine2.setOrderFormat(OrderFormat.PHYSICAL_RESOURCE);

    poLine2.getCost().setQuantityPhysical(currentPQuantity);
    poLine2.getCost().setQuantityElectronic(0);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);

    when(ordersClient.getOrderLineById("id")).thenReturn(poLine2);
    FundCollection fundCollection = new FundCollection()
      .funds(Collections.singletonList(new Fund()))
      .totalRecords(1);
    when(financeClient.getFundsByQuery(anyString())).thenReturn(fundCollection);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    ArgumentCaptor<PoLine> argumentCaptor = ArgumentCaptor.forClass(PoLine.class);
    verify(ordersClient).putOrderLine(any(), argumentCaptor.capture());
    var updatedCompLine = argumentCaptor.getValue();

    assertEquals(newLocationQuantity, updatedCompLine.getLocations().getFirst().getQuantityPhysical());
    assertEquals(1, updatedCompLine.getLocations().size());
  }

  @ParameterizedTest
  @CsvSource({
    "1, 1, 1, 0",
    "2, 1, 2, 0",
    // see https://issues.folio.org/browse/MODEBSNET-18
  })
  @DisplayName("Update line with multiple locations. Electronic")
  void updateLineWithMultipleLocationsElectronic(int ebsconetQuantity, int currentPQuantity, int newLocationQuantity) {
    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("CODE", ebsconetQuantity);

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    PoLine poLine2 = getSampleCompPoLine();

    List<Location> locations = new ArrayList<>();
    locations.add(new Location().locationId(UUID.randomUUID().toString()).quantityPhysical(currentPQuantity));
    locations.add(new Location().locationId(UUID.randomUUID().toString()).quantityPhysical(currentPQuantity));
    poLine2.setLocations(locations);

    poLine2.setOrderFormat(OrderFormat.ELECTRONIC_RESOURCE);

    poLine2.getCost().setQuantityPhysical(currentPQuantity);
    poLine2.getCost().setQuantityElectronic(0);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);

    when(ordersClient.getOrderLineById("id")).thenReturn(poLine2);

    FundCollection fundCollection = new FundCollection()
      .funds(Collections.singletonList(new Fund()))
      .totalRecords(1);
    when(financeClient.getFundsByQuery(anyString())).thenReturn(fundCollection);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    ArgumentCaptor<PoLine> argumentCaptor = ArgumentCaptor.forClass(PoLine.class);
    verify(ordersClient).putOrderLine(any(), argumentCaptor.capture());
    var updatedCompLine = argumentCaptor.getValue();

    assertEquals(newLocationQuantity, updatedCompLine.getLocations().getFirst().getQuantityElectronic());
    assertEquals(1, updatedCompLine.getLocations().size());
  }

  @ParameterizedTest
  @CsvSource({
    "1, 1, 1",
    "2, 1, 2"
    // see https://issues.folio.org/browse/MODEBSNET-18
  })
  @DisplayName("Update line with single location. Physical")
  void updateLineWithSingleLocationPhysical(int ebsconetQuantity, int currentPQuantity, int newLocationQuantity) {

    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("CODE", ebsconetQuantity);

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    PoLine poLine2 = getSampleCompPoLine();

    List<Location> locations = new ArrayList<>();
    var location = new Location()
      .locationId(UUID.randomUUID().toString())
      .quantityPhysical(currentPQuantity)
      .quantityElectronic(0);

    locations.add(location);
    poLine2.setLocations(locations);

    poLine2.setOrderFormat(OrderFormat.PHYSICAL_RESOURCE);

    poLine2.getCost().setQuantityPhysical(currentPQuantity);
    poLine2.getCost().setQuantityElectronic(0);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);

    when(ordersClient.getOrderLineById("id")).thenReturn(poLine2);

    FundCollection fundCollection = new FundCollection()
      .funds(Collections.singletonList(new Fund()))
      .totalRecords(1);
    when(financeClient.getFundsByQuery(anyString())).thenReturn(fundCollection);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    ArgumentCaptor<PoLine> argumentCaptor = ArgumentCaptor.forClass(PoLine.class);
    verify(ordersClient).putOrderLine(any(), argumentCaptor.capture());
    var updatedCompLine = argumentCaptor.getValue();

    assertEquals(newLocationQuantity, updatedCompLine.getLocations().getFirst().getQuantityPhysical());
    assertEquals(0, updatedCompLine.getLocations().getFirst().getQuantityElectronic());
  }

  @ParameterizedTest
  @CsvSource({
    "1, 1, 1",
    "2, 1, 2",
    // see https://issues.folio.org/browse/MODEBSNET-18
  })
  @DisplayName("Update line with single location. Electronic")
  void updateLineWithSingleLocationElectronic(int ebsconetQuantity, int currentEQuantity, int newLocationQuantity) {
    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("CODE", ebsconetQuantity);

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    PoLine poLine2 = getSampleCompPoLine();

    List<Location> locations = new ArrayList<>();
    var location = new Location()
      .locationId(UUID.randomUUID().toString())
      .quantityPhysical(0)
      .quantityElectronic(currentEQuantity);

    locations.add(location);
    poLine2.setLocations(locations);

    poLine2.setOrderFormat(OrderFormat.ELECTRONIC_RESOURCE);

    poLine2.getCost().setQuantityPhysical(0);
    poLine2.getCost().setQuantityElectronic(currentEQuantity);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);

    when(ordersClient.getOrderLineById("id")).thenReturn(poLine2);
    FundCollection fundCollection = new FundCollection()
      .funds(Collections.singletonList(new Fund()))
      .totalRecords(1);
    when(financeClient.getFundsByQuery(anyString())).thenReturn(fundCollection);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    ArgumentCaptor<PoLine> argumentCaptor = ArgumentCaptor.forClass(PoLine.class);
    verify(ordersClient).putOrderLine(any(), argumentCaptor.capture());
    var updatedCompLine = argumentCaptor.getValue();

    assertEquals(newLocationQuantity, updatedCompLine.getLocations().getFirst().getQuantityElectronic());
    assertEquals(0, updatedCompLine.getLocations().getFirst().getQuantityPhysical());
  }

  @Test
  void mapFundWithExpenseClass() {
    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("CODE:Prnt", 1);

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();

    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    var poLine2 = new PoLine();
    var fundDistribution = new FundDistribution();
    fundDistribution.setCode("CODE");
    poLine2.setId(poLine1.getId());
    poLine2.setFundDistribution(Collections.singletonList(fundDistribution));
    poLine2.setCost(new Cost());
    poLine2.setVendorDetail(new VendorDetail());
    poLine2.setDetails(new Details());
    poLine2.setLocations(Collections.singletonList(new Location()));
    poLine2.setOrderFormat(OrderFormat.PHYSICAL_RESOURCE);

    var funds = new FundCollection();
    var fund = new Fund();
    fund.setCode("DIFFERENT_CODE");
    fund.setId("id");
    funds.setFunds(Collections.singletonList(fund));
    funds.setTotalRecords(1);

    var expenseClassCollection = new ExpenseClassCollection();
    var expenseClass = new ExpenseClass();
    expenseClass.setCode("Prnt");
    expenseClass.setId("id");
    expenseClassCollection.setExpenseClasses(Collections.singletonList(expenseClass));
    expenseClassCollection.setTotalRecords(1);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);
    when(ordersClient.getOrderLineById("id")).thenReturn(poLine2);
    when(financeClient.getFundsByQuery(any())).thenReturn(funds);
    when(financeClient.getExpenseClassesByQuery(anyString())).thenReturn(expenseClassCollection);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    verify(ordersClient, times(1)).getOrderLinesByQuery(anyString());
    verify(ordersClient, times(1)).getOrderLineById(anyString());
    verify(ordersClient, times(1)).putOrderLine(anyString(), any());
    verify(financeClient, times(1)).getExpenseClassesByQuery(anyString());
  }

  @ParameterizedTest
  @CsvSource({
    "Awaiting Payment, Partially Received, Cancelled, Cancelled",
    "Partially Paid, Cancelled, Cancelled, Cancelled",
    "Cancelled, Pending, Cancelled, Cancelled",
  })
  void cancelPoLine(String paymentStatus, String receiptStatus, String resultPaymentStatus, String resultReceiptStatus) {
    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("CODE", 1);
    ebsconetOrderLine.setType("non-renewal");

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);

    PoLine poLine2 = getSampleCompPoLine();
    poLine2.setId(poLine1.getId());
    poLine2.setPaymentStatus(PaymentStatus.fromValue(paymentStatus));
    poLine2.setReceiptStatus(ReceiptStatus.fromValue(receiptStatus));

    when(ordersClient.getOrderLineById("id")).thenReturn(poLine2);

    FundCollection fundCollection = new FundCollection()
      .funds(Collections.singletonList(new Fund()))
      .totalRecords(1);
    when(financeClient.getFundsByQuery("code==CODE")).thenReturn(fundCollection);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    verify(ordersClient, times(1)).putOrderLine(anyString(),
      argThat(line -> line.getPaymentStatus() == PaymentStatus.fromValue(resultPaymentStatus) &&
        line.getReceiptStatus() == ReceiptStatus.fromValue(resultReceiptStatus)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Pending", "Open", "Closed", ""})
  void cancelPoLineWithVariableWorkflowStatus(String workflowStatusValue) {
    var workflowStatus = StringUtils.isNotBlank(workflowStatusValue)
      ? WorkflowStatus.fromValue(workflowStatusValue) : null;

    var ebsconetOrderLine = getSampleEbsconetOrderLine("CODE", 1);
    ebsconetOrderLine.setWorkflowStatus(workflowStatus);

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);

    var poLine = getSampleCompPoLine();
    poLine.setId(poLine1.getId());
    poLine.setPaymentStatus(PaymentStatus.AWAITING_PAYMENT);
    poLine.setReceiptStatus(ReceiptStatus.AWAITING_RECEIPT);
    when(ordersClient.getOrderLineById("id")).thenReturn(poLine);

    var fundCollection = new FundCollection()
      .funds(Collections.singletonList(new Fund()))
      .totalRecords(1);
    when(financeClient.getFundsByQuery("code==CODE")).thenReturn(fundCollection);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    verify(ordersClient, times(1)).putOrderLine(anyString(),
      argThat(line -> workflowStatus == WorkflowStatus.CLOSED
        ? (line.getPaymentStatus() == PaymentStatus.CANCELLED && line.getReceiptStatus() == ReceiptStatus.CANCELLED)
        : (line.getPaymentStatus() == PaymentStatus.AWAITING_PAYMENT && line.getReceiptStatus() == ReceiptStatus.AWAITING_RECEIPT)));
  }

  @Test
  void shouldThrowExceptionCannotCancelComplete() {
    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("CODE", 1);
    ebsconetOrderLine.setType("non-renewal");

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    PoLine poLine2 = getSampleCompPoLine();
    poLine2.setPaymentStatus(PaymentStatus.FULLY_PAID);
    poLine2.setReceiptStatus(ReceiptStatus.RECEIPT_NOT_REQUIRED);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);
    when(ordersClient.getOrderLineById("id")).thenReturn(poLine2);

    FundCollection fundCollection = new FundCollection()
      .funds(Collections.singletonList(new Fund()))
      .totalRecords(1);
    when(financeClient.getFundsByQuery("code==CODE")).thenReturn(fundCollection);

    Exception exception = assertThrows(UnprocessableEntity.class,
      () -> ordersService.updateEbscoNetOrderLine(ebsconetOrderLine));
    assertThat(exception.getMessage(), is("Order line was not automatically canceled because it is already complete."));
  }

  @ParameterizedTest
  @CsvSource({
    "Fully Paid, Cancelled",
    "Cancelled, Receipt Not Required",
    "Cancelled, Cancelled",
  })
  void shouldThrowExceptionAlreadyCanceled(String paymentStatus, String receiptStatus) {
    EbsconetOrderLine ebsconetOrderLine = getSampleEbsconetOrderLine("CODE", 1);
    ebsconetOrderLine.setType("non-renewal");

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine1 = new PoLine();
    poLine1.setId("id");
    polResult.addPoLinesItem(poLine1);
    polResult.setTotalRecords(1);

    PoLine poLine2 = getSampleCompPoLine();
    poLine2.setPaymentStatus(PaymentStatus.fromValue(paymentStatus));
    poLine2.setReceiptStatus(ReceiptStatus.fromValue(receiptStatus));

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);
    when(ordersClient.getOrderLineById("id")).thenReturn(poLine2);

    FundCollection fundCollection = new FundCollection()
      .funds(Collections.singletonList(new Fund()))
      .totalRecords(1);
    when(financeClient.getFundsByQuery("code==CODE")).thenReturn(fundCollection);

    Exception exception = assertThrows(UnprocessableEntity.class,
      () -> ordersService.updateEbscoNetOrderLine(ebsconetOrderLine));
    assertThat(exception.getMessage(), is("Order line was not automatically canceled because it is already canceled."));
  }


  private static Stream<Arguments> getPriceParameters() {
    // see https://issues.folio.org/browse/MODEBSNET-10
    // ebscoPrice, currentPPrice, currentEPrice, expectedPPrice, expectedEPrice
    return Stream.of(
      Arguments.of(BigDecimal.valueOf(66.00), BigDecimal.valueOf(10.00), BigDecimal.ZERO, BigDecimal.valueOf(66.00), BigDecimal.ZERO), // P>0, E=0
      Arguments.of(BigDecimal.valueOf(66.00), BigDecimal.valueOf(40.00), BigDecimal.valueOf(10.00), BigDecimal.valueOf(33.00), BigDecimal.valueOf(33.00)), // P>0, E>0
      Arguments.of(BigDecimal.valueOf(66.00), BigDecimal.ZERO, BigDecimal.valueOf(10.00), BigDecimal.ZERO, BigDecimal.valueOf(66.00)),  // P=0, E>0
      Arguments.of(BigDecimal.valueOf(66.00), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)  // P=0, E=0
    );
  }

  private EbsconetOrderLine getSampleEbsconetOrderLine(String fundCode, int quantity) {
    var ebsconetOrderLine = new EbsconetOrderLine();
    ebsconetOrderLine.setFundCode(fundCode);
    ebsconetOrderLine.setPoLineNumber("10000-1");
    ebsconetOrderLine.setCurrency("USD");
    ebsconetOrderLine.setVendor("VENDOR");
    ebsconetOrderLine.setUnitPrice(BigDecimal.ONE);
    ebsconetOrderLine.setQuantity(quantity);
    return ebsconetOrderLine;
  }

  private PoLine getSampleCompPoLine() {
    var poLine = new PoLine();

    var cost = new Cost();
    cost.setQuantityPhysical(4);
    cost.setQuantityElectronic(2);
    cost.setListUnitPrice(BigDecimal.valueOf(11));
    cost.setListUnitPriceElectronic(BigDecimal.valueOf(2));
    poLine.setCost(cost);

    var fundDistribution = new FundDistribution().code("CODE");

    poLine.setFundDistribution(Collections.singletonList(fundDistribution));

    poLine.setVendorDetail(new VendorDetail());
    poLine.setDetails(new Details());
    poLine.setLocations(Collections.singletonList(new Location()));
    poLine.setOrderFormat(OrderFormat.P_E_MIX);
    return poLine;
  }
}
