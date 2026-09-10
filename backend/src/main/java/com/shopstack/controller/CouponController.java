package com.shopstack.controller;

import com.shopstack.dto.coupon.ApplicableCouponResponse;
import com.shopstack.dto.coupon.CouponApplyRequest;
import com.shopstack.dto.coupon.CouponResponse;
import com.shopstack.dto.coupon.CouponValidateRequest;
import com.shopstack.dto.coupon.CouponValidateResponse;
import com.shopstack.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/validate")
    public ResponseEntity<CouponValidateResponse> validateCoupon(@Valid @RequestBody CouponValidateRequest request) {
        CouponValidateResponse response = couponService.validateCoupon(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apply")
    public ResponseEntity<CouponResponse> applyCoupon(@Valid @RequestBody CouponApplyRequest request) {
        CouponResponse response = couponService.applyCoupon(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<CouponResponse> getCoupon(@PathVariable String code) {
        CouponResponse response = couponService.getCouponByCode(code);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/available")
    public ResponseEntity<List<CouponResponse>> getAvailableCoupons() {
        List<CouponResponse> response = couponService.listAvailableCoupons();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/applicable")
    public ResponseEntity<List<ApplicableCouponResponse>> getApplicableCoupons() {
        List<ApplicableCouponResponse> response = couponService.getApplicableCoupons();
        return ResponseEntity.ok(response);
    }
}
