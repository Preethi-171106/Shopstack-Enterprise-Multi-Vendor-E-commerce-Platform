package com.shopstack.controller;

import com.shopstack.dto.coupon.CouponAnalyticsResponse;
import com.shopstack.dto.coupon.CouponCreateRequest;
import com.shopstack.dto.coupon.CouponResponse;
import com.shopstack.dto.coupon.CouponUpdateRequest;
import com.shopstack.dto.coupon.CouponUsageResponse;
import com.shopstack.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CouponCreateRequest request) {
        CouponResponse response = couponService.createCoupon(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CouponResponse> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponUpdateRequest request) {
        CouponResponse response = couponService.updateCoupon(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = {"/{id}/activate", "/{id}/enable"})
    public ResponseEntity<CouponResponse> activateCoupon(@PathVariable Long id) {
        CouponResponse response = couponService.enableCoupon(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping(value = {"/{id}/deactivate", "/{id}/disable"})
    public ResponseEntity<CouponResponse> deactivateCoupon(@PathVariable Long id) {
        CouponResponse response = couponService.disableCoupon(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CouponResponse>> listCoupons(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        List<CouponResponse> response = couponService.listCoupons(search, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics")
    public ResponseEntity<CouponAnalyticsResponse> getCouponAnalytics() {
        CouponAnalyticsResponse response = couponService.getCouponAnalytics();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CouponResponse> getCoupon(@PathVariable Long id) {
        CouponResponse response = couponService.getCoupon(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/usage")
    public ResponseEntity<List<CouponUsageResponse>> getCouponUsage(@PathVariable Long id) {
        List<CouponUsageResponse> response = couponService.getCouponUsages(id);
        return ResponseEntity.ok(response);
    }
}

