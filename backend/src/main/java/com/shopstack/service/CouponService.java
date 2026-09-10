package com.shopstack.service;

import com.shopstack.dto.coupon.ApplicableCouponResponse;
import com.shopstack.dto.coupon.CouponAnalyticsResponse;
import com.shopstack.dto.coupon.CouponApplyRequest;
import com.shopstack.dto.coupon.CouponCreateRequest;
import com.shopstack.dto.coupon.CouponResponse;
import com.shopstack.dto.coupon.CouponUpdateRequest;
import com.shopstack.dto.coupon.CouponUsageResponse;
import com.shopstack.dto.coupon.CouponValidateRequest;
import com.shopstack.dto.coupon.CouponValidateResponse;
import com.shopstack.entity.Coupon;

import java.util.List;

public interface CouponService {

    CouponResponse createCoupon(CouponCreateRequest request);

    CouponResponse updateCoupon(Long id, CouponUpdateRequest request);

    void deleteCoupon(Long id);

    CouponResponse enableCoupon(Long id);

    CouponResponse disableCoupon(Long id);

    CouponResponse getCoupon(Long id);

    CouponResponse getCouponByCode(String code);

    List<CouponResponse> listCoupons();

    List<CouponResponse> listCoupons(String search, String status);

    List<CouponResponse> listAvailableCoupons();

    List<ApplicableCouponResponse> getApplicableCoupons();

    List<ApplicableCouponResponse> getApplicableCoupons(String userEmail);

    CouponResponse applyCoupon(CouponApplyRequest request);

    CouponValidateResponse validateCoupon(CouponValidateRequest request);

    void validateCoupon(Coupon coupon, String userEmail);

    void recordCouponUsage(Coupon coupon, Long orderId, String userEmail);

    List<CouponUsageResponse> getCouponUsages(Long couponId);

    CouponAnalyticsResponse getCouponAnalytics();
}

