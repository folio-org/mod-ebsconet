package org.folio.ebsconet.mapper;

import org.folio.ebsconet.domain.dto.CompositePoLine;
import org.folio.ebsconet.domain.dto.Cost;
import org.folio.ebsconet.domain.dto.EbsconetOrderLine;
import org.folio.ebsconet.domain.dto.FundDistribution;
import org.folio.ebsconet.domain.dto.Location;
import org.folio.ebsconet.domain.dto.Organization;
import org.folio.ebsconet.domain.dto.PaymentStatus;
import org.folio.ebsconet.domain.dto.PoLine;
import org.folio.ebsconet.domain.dto.PurchaseOrder;
import org.folio.ebsconet.domain.dto.ReceiptStatus;
import org.folio.ebsconet.domain.dto.Source;
import org.folio.ebsconet.error.UnprocessableEntity;
import org.folio.ebsconet.models.MappingDataHolder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class OrdersMapper {

  static final String CANNOT_CANCEL_BECAUSE_COMPLETE =
    "Order line was not automatically canceled because it is already complete.";
  static final String CANNOT_CANCEL_BECAUSE_ALREADY_CANCELED =
    "Order line was not automatically canceled because it is already canceled.";
  private static final BigDecimal ONE_HUNDRED_PERCENT = BigDecimal.valueOf(100);

  @Mapping(target = "vendor", source = "vendor.code")
  @Mapping(target = "cancellationRestriction", source = "line.cancellationRestriction")
  @Mapping(target = "cancellationRestrictionNote", source = "line.cancellationRestrictionNote")
  @Mapping(target = "unitPrice", source = "line", qualifiedByName = "getUnitPrice")
  @Mapping(target = "currency", source = "line.cost.currency")
  @Mapping(target = "vendorReferenceNumbers", source = "line.vendorDetail.referenceNumbers")
  @Mapping(target = "poLineNumber", source = "line.poLineNumber")
  @Mapping(target = "subscriptionToDate", source = "line.details.subscriptionTo")
  @Mapping(target = "subscriptionFromDate", source = "line.details.subscriptionFrom")
  @Mapping(target = "quantity", source = "line", qualifiedByName = "getQuantity")
  @Mapping(target = "fundCode", source = "line", qualifiedByName = "getFundCode")
  @Mapping(target = "publisherName", source = "line.publisher")
  @Mapping(target = "vendorAccountNumber", source = "line.vendorDetail.vendorAccount")
  @Mapping(target = "workflowStatus", source = "order.workflowStatus")
  @Mapping(target = "internalNote", source = "line.renewalNote")
  public abstract EbsconetOrderLine folioToEbsconet(PurchaseOrder order, PoLine line, Organization vendor);

  @Named("getFundCode")
  public String getFundCode(PoLine line) {
    List<FundDistribution> distributions = line.getFundDistribution();
    if (distributions == null || distributions.isEmpty())
      return null;
    return distributions.get(0).getCode();
  }

  @Named("getQuantity")
  public Integer getQuantity(PoLine line) {
    Integer physical = Optional.ofNullable(line.getCost()).map(Cost::getQuantityPhysical).orElse(0);
    Integer electronic = Optional.ofNullable(line.getCost()).map(Cost::getQuantityElectronic).orElse(0);
    return physical + electronic;
  }

  @Named("getUnitPrice")
  public BigDecimal getUnitPrice(PoLine line) {
    BigDecimal physical = Optional.ofNullable(line.getCost()).map(Cost::getListUnitPrice).orElse(BigDecimal.ZERO);
    BigDecimal electronic = Optional.ofNullable(line.getCost()).map(Cost::getListUnitPriceElectronic).orElse(BigDecimal.ZERO);
    return physical.add(electronic);
  }

  public void ebsconetToFolio(MappingDataHolder mappingDataHolder) {
    var poLine = mappingDataHolder.getCompositePoLine();
    var ebsconetOrderLine = mappingDataHolder.getEbsconetOrderLine();
    var fund = mappingDataHolder.getFund();

    poLine.setSource(Source.EBSCONET);
    poLine.setCancellationRestriction(ebsconetOrderLine.getCancellationRestriction());
    poLine.setCancellationRestrictionNote(ebsconetOrderLine.getCancellationRestrictionNote());
    poLine.getCost().setCurrency(ebsconetOrderLine.getCurrency());
    poLine.getVendorDetail().setReferenceNumbers(ebsconetOrderLine.getVendorReferenceNumbers());
    poLine.getDetails().setSubscriptionTo(ebsconetOrderLine.getSubscriptionToDate());
    poLine.getDetails().setSubscriptionFrom(ebsconetOrderLine.getSubscriptionFromDate());
    poLine.getVendorDetail().setVendorAccount(ebsconetOrderLine.getVendorAccountNumber());
    poLine.setPublisher(ebsconetOrderLine.getPublisherName());
    poLine.setRenewalNote(mappingDataHolder.getEbsconetOrderLine().getInternalNote());

    populateCostAndLocations(poLine, ebsconetOrderLine);
    removeZeroAmountLocations(poLine);

    if (ebsconetOrderLine.getType() != null && ebsconetOrderLine.getType().equalsIgnoreCase("non-renewal")) {
        cancelOrderLine(poLine);
    }

    if (fund != null) {
      var fundDistribution = new FundDistribution()
        .fundId(fund.getId())
        .code(fund.getCode())
        .expenseClassId(mappingDataHolder.getExpenseClass())
        .distributionType(FundDistribution.DistributionTypeEnum.PERCENTAGE)
        .value(ONE_HUNDRED_PERCENT);

      poLine.fundDistribution(Collections.singletonList(fundDistribution));
    }
  }

  private void populateCostAndLocations(CompositePoLine poLine, EbsconetOrderLine ebsconetOrderLine) {
    switch (poLine.getOrderFormat()) {
      case ELECTRONIC_RESOURCE -> populateElectronicCostAndLocation(poLine, ebsconetOrderLine);
      case OTHER, PHYSICAL_RESOURCE -> populatePhysicalCostAndLocation(poLine, ebsconetOrderLine);
      case P_E_MIX -> populateCostAndLocationPEMix(poLine, ebsconetOrderLine);
    }
  }

  private void populatePhysicalCostAndLocation(CompositePoLine poLine, EbsconetOrderLine ebsconetOrderLine) {
    poLine.getCost().setQuantityPhysical(ebsconetOrderLine.getQuantity());
    poLine.getCost().setListUnitPrice(ebsconetOrderLine.getUnitPrice());

    clearLocationQuantities(poLine);

    poLine.getLocations().get(0).setQuantityPhysical(ebsconetOrderLine.getQuantity());
  }



  private void populateElectronicCostAndLocation(CompositePoLine poLine, EbsconetOrderLine ebsconetOrderLine) {
    poLine.getCost().setQuantityElectronic(ebsconetOrderLine.getQuantity());
    poLine.getCost().setListUnitPriceElectronic(ebsconetOrderLine.getUnitPrice());

    clearLocationQuantities(poLine);

    poLine.getLocations().get(0).setQuantityElectronic(ebsconetOrderLine.getQuantity());
  }

  private void populateCostAndLocationPEMix(CompositePoLine poLine, EbsconetOrderLine ebsconetOrderLine) {
    processPEmixPriceUpdate(poLine, ebsconetOrderLine);
    processPEMixQuantityUpdate(poLine, ebsconetOrderLine);
  }

  private void processPEMixQuantityUpdate(CompositePoLine poLine, EbsconetOrderLine ebsconetOrderLine) {
    // special case. 1 ebsconet item for PE mix = 1 physical + 1 electronic folio items
    if (ebsconetOrderLine.getQuantity() == 1) {
      redistributeSinglePeMixItem(poLine);
    }

    // Q physical = 1 and electronic > 1
    else if (poLine.getCost().getQuantityPhysical() == 1 && poLine.getCost().getQuantityElectronic() > 1) {
      poLine.getCost().setQuantityElectronic(ebsconetOrderLine.getQuantity() - 1);
      poLine.getLocations().get(0).setQuantityElectronic(ebsconetOrderLine.getQuantity() - 1);
      poLine.getCost().setQuantityPhysical(1);
      poLine.getLocations().get(0).setQuantityPhysical(1);
    }

    // Q physical > 1, Q electronic = 1
    else if (poLine.getCost().getQuantityPhysical() > 1 && poLine.getCost().getQuantityElectronic() == 1) {
      poLine.getCost().setQuantityPhysical(ebsconetOrderLine.getQuantity() - 1);
      poLine.getLocations().get(0).setQuantityPhysical(ebsconetOrderLine.getQuantity() - 1);
      poLine.getCost().setQuantityElectronic(1);
      poLine.getLocations().get(0).setQuantityElectronic(1);
    }

    // Q (physical > 1 and Q electronic > 1)  OR  (physical = 1 and electronic = 1)
    else if ((poLine.getCost().getQuantityElectronic() > 1 && poLine.getCost().getQuantityPhysical() > 1)
      || (poLine.getCost().getQuantityElectronic() == 1 && poLine.getCost().getQuantityPhysical() == 1)) {
      int newElectronicQuantity = ebsconetOrderLine.getQuantity() / 2;
      int newPhysicalQuantity = ebsconetOrderLine.getQuantity() - newElectronicQuantity;

      poLine.getCost().setQuantityElectronic(newElectronicQuantity);
      poLine.getCost().setQuantityPhysical(newPhysicalQuantity);
      poLine.getLocations().get(0).setQuantityElectronic(newElectronicQuantity);
      poLine.getLocations().get(0).setQuantityPhysical(newPhysicalQuantity);
    }
  }

  private void redistributeSinglePeMixItem(CompositePoLine poLine) {
    poLine.getCost().setQuantityElectronic(1);
    poLine.getCost().setQuantityPhysical(1);

    // set all location quantities to zero
    clearLocationQuantities(poLine);

    // redistribute quantities for single and multiple locations
    if (poLine.getLocations().size() == 1) {
      poLine.getLocations().get(0).setQuantityPhysical(1);
      poLine.getLocations().get(0).setQuantityElectronic(1);
    } else {
      poLine.getLocations().get(0).setQuantityPhysical(1);
      poLine.getLocations().get(1).setQuantityElectronic(1);
    }

  }

  private void processPEmixPriceUpdate(CompositePoLine poLine, EbsconetOrderLine ebsconetOrderLine) {
    var fractionDigits = Currency.getInstance(ebsconetOrderLine.getCurrency()).getDefaultFractionDigits();
    var unitPrice = ebsconetOrderLine.getUnitPrice().setScale(fractionDigits, RoundingMode.HALF_EVEN);

    // Price physical = 0 and electronic > 0
    if (poLine.getCost().getListUnitPrice().signum() == 0 && poLine.getCost().getListUnitPriceElectronic().signum() > 0) {
      poLine.getCost().setListUnitPriceElectronic(unitPrice);
    }

    // Price physical > 0, electronic = 0
    else if (poLine.getCost().getListUnitPriceElectronic().signum() == 0 && poLine.getCost().getListUnitPrice().signum() > 0) {
      poLine.getCost().setListUnitPrice(unitPrice);
    }
    // Price physical > 0, electronic > 0
    else if (poLine.getCost().getListUnitPriceElectronic().signum() > 0 && poLine.getCost().getListUnitPrice().signum() > 0) {
      // divide unit price

      BigDecimal newElectronicPrice = unitPrice.divide(BigDecimal.valueOf(2), fractionDigits, RoundingMode.HALF_EVEN).setScale(fractionDigits, RoundingMode.HALF_EVEN);
      BigDecimal newPhysicalPrice = unitPrice.subtract(newElectronicPrice).setScale(fractionDigits, RoundingMode.HALF_EVEN);

      poLine.getCost().setListUnitPriceElectronic(newElectronicPrice);
      poLine.getCost().setListUnitPrice(newPhysicalPrice);
    }
  }


  private void removeZeroAmountLocations(CompositePoLine poLine) {
    Predicate<Location> locationQuantityGreaterThanZero = location ->
      location.getQuantityElectronic() != null && location.getQuantityElectronic() > 0
        || location.getQuantityPhysical() != null && location.getQuantityPhysical() > 0;

    var nonZeroLocations = poLine.getLocations()
      .stream()
      .filter(locationQuantityGreaterThanZero)
      .toList();

    poLine.setLocations(nonZeroLocations);
  }


  private void clearLocationQuantities(CompositePoLine poLine) {
    poLine.getLocations().forEach(location -> {
      location.setQuantityPhysical(0);
      location.setQuantityElectronic(0);
    });
  }

  private void cancelOrderLine(CompositePoLine poLine) {
    List<PaymentStatus> paymentResolved = List.of(PaymentStatus.PAYMENT_NOT_REQUIRED, PaymentStatus.FULLY_PAID,
      PaymentStatus.CANCELLED);
    List<ReceiptStatus> receiptResolved = List.of(ReceiptStatus.RECEIPT_NOT_REQUIRED, ReceiptStatus.FULLY_RECEIVED,
      ReceiptStatus.CANCELLED);
    PaymentStatus paymentStatus = poLine.getPaymentStatus();
    ReceiptStatus receiptStatus = poLine.getReceiptStatus();
    if (paymentResolved.contains(paymentStatus) && paymentStatus != PaymentStatus.CANCELLED &&
        receiptResolved.contains(receiptStatus) && receiptStatus != ReceiptStatus.CANCELLED) {
      throw new UnprocessableEntity(CANNOT_CANCEL_BECAUSE_COMPLETE);
    }
    if ((paymentResolved.contains(paymentStatus) && receiptStatus == ReceiptStatus.CANCELLED) ||
        (paymentStatus == PaymentStatus.CANCELLED && receiptResolved.contains(receiptStatus))) {
      throw new UnprocessableEntity(CANNOT_CANCEL_BECAUSE_ALREADY_CANCELED);
    }
    if (!paymentResolved.contains(paymentStatus))
      poLine.setPaymentStatus(PaymentStatus.CANCELLED);
    if (!receiptResolved.contains(receiptStatus))
      poLine.setReceiptStatus(ReceiptStatus.CANCELLED);
  }
}
