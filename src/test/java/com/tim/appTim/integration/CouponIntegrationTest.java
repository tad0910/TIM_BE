package com.tim.appTim.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tim.appTim.entity.Coupon;
import com.tim.appTim.repository.CouponRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CouponIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CouponRepository couponRepository;

    private final String BASE_URL = "/api/coupons";

    private Coupon createValidCoupon() {
        Coupon coupon = new Coupon();
        coupon.setCode("TEST2025");
        coupon.setStartDate(LocalDate.now());
        coupon.setEndDate(LocalDate.now().plusDays(30));
        coupon.setDiscountValue(BigDecimal.valueOf(100000));
        coupon.setDiscountType(Coupon.DiscountType.AMOUNT);
        coupon.setScenario(Coupon.CouponScenario.DEDUCT_FIRST_FULL);
        coupon.setQuantity(100);
        return coupon;
    }

    @Test
    @WithMockUser(authorities = "tuition:read")
    void getAllCoupons_WhenAuthorized_ShouldReturn200() throws Exception {
        Coupon coupon = createValidCoupon();
        coupon.setId(1L);
        doReturn(List.of(coupon)).when(couponRepository).findAll();

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TEST2025"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCoupons_WhenAdmin_ShouldReturn200() throws Exception {
        doReturn(List.of()).when(couponRepository).findAll();

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "user:read")
    void getAllCoupons_WhenUnauthorized_ShouldReturn403() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "tuition:create")
    void createCoupon_WhenAuthorized_ShouldReturn200() throws Exception {
        Coupon coupon = createValidCoupon();
        Coupon savedCoupon = createValidCoupon();
        savedCoupon.setId(1L);

        doReturn(false).when(couponRepository).existsByCode(coupon.getCode());
        doReturn(savedCoupon).when(couponRepository).save(any(Coupon.class));

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(coupon)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.code").value("TEST2025"));
    }

    @Test
    @WithMockUser(authorities = "tuition:create")
    void createCoupon_WhenDuplicateCode_ShouldReturn500() throws Exception {
        Coupon coupon = createValidCoupon();
        doReturn(true).when(couponRepository).existsByCode(coupon.getCode());

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(coupon)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Lỗi hệ thống: Mã giảm giá này đã tồn tại!"));
    }

    @Test
    @WithMockUser(authorities = "user:read")
    void createCoupon_WhenUnauthorized_ShouldReturn403() throws Exception {
        Coupon coupon = createValidCoupon();
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(coupon)))
                .andExpect(status().isForbidden());
    }
}
