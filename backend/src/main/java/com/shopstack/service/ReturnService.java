package com.shopstack.service;

import com.shopstack.dto.returns.QualityCheckRequest;
import com.shopstack.dto.returns.RefundProcessRequest;
import com.shopstack.dto.returns.ReturnDecisionRequest;
import com.shopstack.dto.returns.ReturnReceiveRequest;
import com.shopstack.dto.returns.ReturnRequestCreateRequest;
import com.shopstack.dto.returns.ReturnResponse;

import java.util.List;

public interface ReturnService {

    ReturnResponse createReturnRequest(Long orderId, ReturnRequestCreateRequest request);

    ReturnResponse approveReturn(Long id, ReturnDecisionRequest request);

    ReturnResponse rejectReturn(Long id, ReturnDecisionRequest request);

    ReturnResponse markItemReceived(Long id);

    ReturnResponse receiveReturnAtWarehouse(Long id, ReturnReceiveRequest request);

    ReturnResponse performQualityCheck(Long id, QualityCheckRequest request);

    ReturnResponse initiateRefund(Long id, RefundProcessRequest request);

    ReturnResponse getReturn(Long id);

    ReturnResponse getReturnByNumber(String returnNumber);

    List<ReturnResponse> listCustomerReturns();

    List<ReturnResponse> listAllReturns();

    List<ReturnResponse> getWarehouseReturns(Long warehouseId);

    List<ReturnResponse> getPendingReceivingReturns(Long warehouseId);

    List<ReturnResponse> getPendingQcReturns(Long warehouseId);
}
