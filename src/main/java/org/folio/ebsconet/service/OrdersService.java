package org.folio.ebsconet.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.ebsconet.client.FinanceClient;
import org.folio.ebsconet.client.OrdersClient;
import org.folio.ebsconet.client.OrganizationClient;
import org.folio.ebsconet.domain.dto.EbsconetOrderLine;
import org.folio.ebsconet.domain.dto.FundCollection;
import org.folio.ebsconet.domain.dto.PoLine;
import org.folio.ebsconet.domain.dto.PoLineCollection;
import org.folio.ebsconet.domain.dto.PurchaseOrder;
import org.folio.ebsconet.error.ResourceNotFoundException;
import org.folio.ebsconet.mapper.OrdersMapper;
import org.folio.ebsconet.models.MappingDataHolder;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrdersService {
  private final OrdersClient ordersClient;
  private final FinanceClient financeClient;
  private final OrdersMapper ordersMapper = Mappers.getMapper(OrdersMapper.class);
  private final OrganizationClient organizationClient;
  private static final String PO_LINE_NOT_FOUND_MESSAGE = "PO Line not found: ";
  public static final String FUND_CODE_EXPENSE_CLASS_SEPARATOR = ":";


  public EbsconetOrderLine getEbscoNetOrderLine(String poLineNumber) {
    log.info("starting getEbscoNetOrderLine poLineNumber={}", poLineNumber);
    PoLineCollection queryResult;
    try {
      queryResult = ordersClient.getOrderLinesByQuery("poLineNumber==" + poLineNumber);
    } catch (FeignException.NotFound e) {
      throw new ResourceNotFoundException(PO_LINE_NOT_FOUND_MESSAGE + poLineNumber);
    }
    if (queryResult.getTotalRecords() < 1)
      throw new ResourceNotFoundException(PO_LINE_NOT_FOUND_MESSAGE + poLineNumber);
    PoLine line = queryResult.getPoLines().get(0);
    log.debug("order line received for getEbsconetOrderLine poLineNumber={}", poLineNumber);
    PurchaseOrder order = ordersClient.getOrderById(line.getPurchaseOrderId());
    log.debug("order received for getEbsconetOrderLine poLineNumber={}", poLineNumber);
    String vendorId = order.getVendor();
    var vendor = organizationClient.getOrganizationById(vendorId);
    log.debug("Vendor organization received for getEbsconetOrderLine poLineNumber={}", poLineNumber);
    EbsconetOrderLine eol = ordersMapper.folioToEbsconet(order, line, vendor);
    log.info("success for getEbsconetOrderLine: {}", eol);
    return eol;
  }

  public void updateEbscoNetOrderLine(EbsconetOrderLine updateOrderLine) {
    MappingDataHolder mappingDataHolder = new MappingDataHolder();
    mappingDataHolder.setEbsconetOrderLine(updateOrderLine);

    retrievePoLineData(updateOrderLine, mappingDataHolder);
    retrieveFinanceData(updateOrderLine, mappingDataHolder);

    // Convert ebsconet dto to poLine
    ordersMapper.ebsconetToFolio(mappingDataHolder);
    log.info("compositePoLine: {}", mappingDataHolder.getCompositePoLine());

    ordersClient.putOrderLine(mappingDataHolder.getCompositePoLine().getId(), mappingDataHolder.getCompositePoLine());
  }

  private void retrieveFinanceData(EbsconetOrderLine updateOrderLine, MappingDataHolder mappingDataHolder) {
    // Retrieve fund for update if needed
    if (!mappingDataHolder.getCompositePoLine().getFundDistribution().isEmpty()
        && !mappingDataHolder.getCompositePoLine().getFundDistribution().get(0).getCode().equals(updateOrderLine.getFundCode())) {

      FundCollection funds = financeClient.getFundsByQuery("code==" + extractFundCode(updateOrderLine.getFundCode()));

      if (funds.getTotalRecords() < 1) {
        throw new ResourceNotFoundException("Fund not found for: " + updateOrderLine.getFundCode());
      }
      mappingDataHolder.setFund(funds.getFunds().get(0));

      // Retrieve expense class for update
      var expenseClassCode = extractExpenseClassFromFundCode(updateOrderLine.getFundCode());
      if (!StringUtils.isEmpty(expenseClassCode)) {
        var expenseClassCollection = financeClient.getExpenseClassesByQuery("code==" + expenseClassCode);
        if (expenseClassCollection.getTotalRecords() < 1) {
          throw new ResourceNotFoundException("Expense Class not found for: " + updateOrderLine.getFundCode());
        }
        mappingDataHolder.setExpenseClass(expenseClassCollection.getExpenseClasses().get(0).getId());
      }
    }
  }

  private void retrievePoLineData(EbsconetOrderLine updateOrderLine, MappingDataHolder mappingDataHolder) {
    PoLineCollection poLines;
    try {
      poLines = ordersClient.getOrderLinesByQuery("poLineNumber==" + updateOrderLine.getPoLineNumber());
    } catch (Exception e) {
      throw new ResourceNotFoundException(PO_LINE_NOT_FOUND_MESSAGE + updateOrderLine.getPoLineNumber());
    }
    if (poLines.getTotalRecords() < 1) {
      throw new ResourceNotFoundException(PO_LINE_NOT_FOUND_MESSAGE + updateOrderLine.getPoLineNumber());
    }

    var poLine = poLines.getPoLines().get(0);

    mappingDataHolder.setCompositePoLine(ordersClient.getOrderLineById(poLine.getId()));

    if (mappingDataHolder.getCompositePoLine() == null) {
      throw new ResourceNotFoundException(PO_LINE_NOT_FOUND_MESSAGE + poLine.getPoLineNumber());
    }
  }

  public static String extractFundCode(String fundCode) {
    return StringUtils.substringBefore(fundCode, FUND_CODE_EXPENSE_CLASS_SEPARATOR);
  }
  public static String extractExpenseClassFromFundCode(String fundCode) {
    return StringUtils.substringAfterLast(fundCode, FUND_CODE_EXPENSE_CLASS_SEPARATOR);
  }
}
