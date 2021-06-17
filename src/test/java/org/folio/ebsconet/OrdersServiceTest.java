package org.folio.ebsconet;

import feign.FeignException;
import feign.Request;
import feign.Request.Body;
import feign.Request.HttpMethod;
import feign.RequestTemplate;
import java.util.Collections;
import java.util.HashMap;
import org.folio.ebsconet.client.FinanceClient;
import org.folio.ebsconet.client.OrdersClient;
import org.folio.ebsconet.client.OrganizationClient;
import org.folio.ebsconet.domain.dto.*;
import org.folio.ebsconet.error.ResourceNotFoundException;
import org.folio.ebsconet.service.OrdersService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdersServiceTest {
  @Mock
  private OrdersClient ordersClient;
  @Mock
  private FinanceClient financeClient;
  @Mock
  private OrganizationClient organizationClient;
  @InjectMocks
  private OrdersService ordersService;

  private PoLine preparePoLine(String poLineNumber) {
    var pol = new PoLine();
    pol.setId("c0d08448-347b-418a-8c2f-5fb50248d67e");
    pol.setCancellationRestriction(false);
    pol.setCancellationRestrictionNote("Cancellation Restriction Note");
    var cost = new Cost();
    cost.setListUnitPrice(new BigDecimal(1));
    cost.setCurrency("USD");
    cost.setQuantityPhysical(1);
    pol.setCost(cost);
    var details = new Details();
    Date from = Date.from(Instant.parse("2021-03-30T16:00:00.000Z"));
    details.setSubscriptionFrom(JsonNullable.of(from));
    details.setSubscriptionTo(JsonNullable.undefined());
    var fundDistribution = new FundDistribution();
    fundDistribution.setCode("HIST");
    pol.setFundDistribution(List.of(fundDistribution));
    pol.setPoLineNumber(poLineNumber);
    pol.setPublisher("MIT Press");
    pol.setPurchaseOrderId("d79b0bcc-DcAD-1E4E-Abb7-DbFcaD5BB3bb");
    var vendorDetail = new VendorDetail();
    vendorDetail.setVendorAccount("1234-56");
    var refNum = new ReferenceNumberItem();
    refNum.setRefNumber("123456-78");
    refNum.setRefNumberType(ReferenceNumberItem.RefNumberTypeEnum.TITLE_NUMBER);
    vendorDetail.setReferenceNumbers(List.of(refNum));
    pol.setVendorDetail(vendorDetail);
    return pol;
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
  void testGetEbsconetOrderLine() {
    String poLineNumber = "268758-03";
    PoLine pol = preparePoLine(poLineNumber);

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

    verify(ordersClient, times(1)).getOrderLinesByQuery(isA(String.class));
    verify(ordersClient, times(1)).getOrderById(isA(String.class));
    verify(organizationClient, times(1)).getOrganizationById(isA(String.class));

    assertThat(ebsconetOL.getFundCode(), is("HIST"));
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
  void testGetEbsconetOrderLineWithNoFundDistributionCode() {
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
  void shouldCallPutIfCallUpdateEbsconetOrderLineWithElectronic(){
    var ebsconetOrderLine = new EbsconetOrderLine();
    ebsconetOrderLine.setFundCode("CODE");
    ebsconetOrderLine.setPoLineNumber("10000-1");
    ebsconetOrderLine.setCurrency("USD");
    ebsconetOrderLine.setVendor("VENDOR");
    ebsconetOrderLine.setUnitPrice(BigDecimal.ONE);
    ebsconetOrderLine.setQuantity(1);

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine = new PoLine();
    poLine.setId("id");
    polResult.addPoLinesItem(poLine);
    polResult.setTotalRecords(1);

    var compositePoLine = new CompositePoLine();
    var fundDistribution = new FundDistribution();
    fundDistribution.setCode("CODE");
    compositePoLine.setFundDistribution(Collections.singletonList(fundDistribution));
    compositePoLine.setCost(new Cost());
    compositePoLine.setVendorDetail(new VendorDetail());
    compositePoLine.setDetails(new Details());
    compositePoLine.setLocations(Collections.singletonList(new Location()));
    compositePoLine.setOrderFormat(OrderFormat.ELECTRONIC_RESOURCE);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);
    when(ordersClient.getOrderLineById("id")).thenReturn(compositePoLine);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    verify(ordersClient, times(1)).getOrderLinesByQuery(anyString());
    verify(ordersClient, times(1)).getOrderLineById(anyString());
    verify(ordersClient, times(1)).putOrderLine(anyString(),any());
    verify(financeClient, never()).getFundsByQuery(any());
  }

  @Test
  void shouldCallPutIfCallUpdateEbsconetOrderLine(){
    var ebsconetOrderLine = new EbsconetOrderLine();
    ebsconetOrderLine.setFundCode("CODE");
    ebsconetOrderLine.setPoLineNumber("10000-1");
    ebsconetOrderLine.setCurrency("USD");
    ebsconetOrderLine.setVendor("VENDOR");
    ebsconetOrderLine.setUnitPrice(BigDecimal.ONE);
    ebsconetOrderLine.setQuantity(1);

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine = new PoLine();
    poLine.setId("id");
    polResult.addPoLinesItem(poLine);
    polResult.setTotalRecords(1);

    var compositePoLine = new CompositePoLine();
    var fundDistribution = new FundDistribution();
    fundDistribution.setCode("CODE");
    compositePoLine.setFundDistribution(Collections.singletonList(fundDistribution));
    compositePoLine.setCost(new Cost());
    compositePoLine.setVendorDetail(new VendorDetail());
    compositePoLine.setDetails(new Details());
    compositePoLine.setLocations(Collections.singletonList(new Location()));
    compositePoLine.setOrderFormat(OrderFormat.PHYSICAL_RESOURCE);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);
    when(ordersClient.getOrderLineById("id")).thenReturn(compositePoLine);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    verify(ordersClient, times(1)).getOrderLinesByQuery(anyString());
    verify(ordersClient, times(1)).getOrderLineById(anyString());
    verify(ordersClient, times(1)).putOrderLine(anyString(),any());
    verify(financeClient, never()).getFundsByQuery(any());
  }

  @Test
  void shouldCallPutIfCallUpdateEbsconetOrderLineWithDifferentFundCode(){
    var ebsconetOrderLine = new EbsconetOrderLine();
    ebsconetOrderLine.setFundCode("DIFFERENT_CODE");
    ebsconetOrderLine.setPoLineNumber("10000-1");
    ebsconetOrderLine.setCurrency("USD");
    ebsconetOrderLine.setVendor("VENDOR");
    ebsconetOrderLine.setUnitPrice(BigDecimal.ONE);
    ebsconetOrderLine.setQuantity(1);

    var poLineNumber = "10000-1";
    var polResult = new PoLineCollection();
    var poLine = new PoLine();
    poLine.setId("id");
    polResult.addPoLinesItem(poLine);
    polResult.setTotalRecords(1);

    var compositePoLine = new CompositePoLine();
    var fundDistribution = new FundDistribution();
    fundDistribution.setCode("CODE");
    compositePoLine.setFundDistribution(Collections.singletonList(fundDistribution));
    compositePoLine.setCost(new Cost());
    compositePoLine.setVendorDetail(new VendorDetail());
    compositePoLine.setDetails(new Details());
    compositePoLine.setLocations(Collections.singletonList(new Location()));
    compositePoLine.setOrderFormat(OrderFormat.PHYSICAL_RESOURCE);


    var funds = new FundCollection();
    var fund = new Fund();
    fund.setCode("DIFFERENT_CODE");
    fund.setId("id");
    funds.setFunds(Collections.singletonList(fund));
    funds.setTotalRecords(1);

    when(ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber)).thenReturn(polResult);
    when(ordersClient.getOrderLineById("id")).thenReturn(compositePoLine);
    when(financeClient.getFundsByQuery(any())).thenReturn(funds);

    ordersService.updateEbscoNetOrderLine(ebsconetOrderLine);

    verify(ordersClient, times(1)).getOrderLinesByQuery(anyString());
    verify(ordersClient, times(1)).getOrderLineById(anyString());
    verify(ordersClient, times(1)).putOrderLine(anyString(),any());
    verify(financeClient, times(1)).getFundsByQuery(any());
  }

  @Test
  void shouldThrowExceptionIfPoLineNotFound() {
    var poline = new EbsconetOrderLine();
    poline.setPoLineNumber("1");

    Request request = Request.create(HttpMethod.GET, "", new HashMap<>(), Body.empty(), new RequestTemplate());
    when(ordersClient.getOrderLinesByQuery(any())).thenThrow(new FeignException.NotFound("", request, "".getBytes()));
    ResourceNotFoundException resourceNotFoundException = assertThrows(ResourceNotFoundException.class,
      () -> ordersService.updateEbscoNetOrderLine(poline));
    assertThat(resourceNotFoundException.getMessage(),is("PO Line not found: 1"));
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
    assertThat(resourceNotFoundException.getMessage(),is("PO Line not found: 1"));
  }
}
