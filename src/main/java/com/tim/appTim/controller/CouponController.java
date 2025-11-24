package com.tim.appTim.controller;

import com.tim.appTim.entity.Coupon;
import com.tim.appTim.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    @Autowired
    private CouponRepository couponRepository;

    @PostMapping
    @PreAuthorize("hasAuthority('tuition:create') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Coupon> createCoupon(@RequestBody Coupon coupon) {
        if (couponRepository.existsByCode(coupon.getCode())) {
            throw new RuntimeException("Mã giảm giá này đã tồn tại!");
        }
        return ResponseEntity.ok(couponRepository.save(coupon));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('tuition:read') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        return ResponseEntity.ok(couponRepository.findAll());
    }
}