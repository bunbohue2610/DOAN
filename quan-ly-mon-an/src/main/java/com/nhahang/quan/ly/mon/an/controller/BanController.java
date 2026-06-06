package com.nhahang.quan.ly.mon.an.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhahang.quan.ly.mon.an.dto.ApiResponse;
import com.nhahang.quan.ly.mon.an.entity.Ban;
import com.nhahang.quan.ly.mon.an.entity.TaiKhoan;
import com.nhahang.quan.ly.mon.an.repository.BanRepository;
import com.nhahang.quan.ly.mon.an.repository.TaiKhoanRepository;

@RestController
@RequestMapping("/api/ban")
@CrossOrigin("*")
public class BanController {
    private static final Logger logger = LoggerFactory.getLogger(BanController.class);

    @Autowired
    private BanRepository banRepo;
    
    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    /**
     * Lấy danh sách bàn ăn của tài khoản hiện tại
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Ban>>> layTatCaBan(@RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        if (userId != null) {
            return ResponseEntity.ok(ApiResponse.ok(banRepo.findByTaiKhoanId(userId)));
        }
        return ResponseEntity.ok(ApiResponse.ok(java.util.Collections.emptyList()));
    }

    /**
     * Tạo bàn mới
     */
    @PostMapping("/tao-ban")
public ResponseEntity<ApiResponse<Ban>> taoBan(@RequestBody Ban ban, 
        @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
    try {
        if (ban == null || ban.getSoBan() == null || ban.getSoBan().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Tên bàn không được để trống!"));
        }

        String soBan = ban.getSoBan().trim();

        if (soBan.length() > 50) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Tên bàn không được vượt quá 50 ký tự!"));
        }

        // ✅ Kiểm tra trùng theo từng tài khoản
        if (userId != null && banRepo.existsByTaiKhoanIdAndSoBan(userId, soBan)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.fail("Số bàn '" + soBan + "' đã tồn tại!"));
        }

        if (ban.getTrangThai() == null) {
            ban.setTrangThai(Ban.TrangThaiBan.TRONG);
        }

        if (userId != null) {
            TaiKhoan taiKhoan = taiKhoanRepository.findById(userId).orElse(null);
            if (taiKhoan != null) {
                ban.setTaiKhoan(taiKhoan);
            }
        }

        ban.setSoBan(soBan);
        Ban banMoi = banRepo.save(ban);
        return ResponseEntity.ok(ApiResponse.ok("Tạo bàn '" + banMoi.getSoBan() + "' thành công!", banMoi));

    } catch (Exception e) {
        logger.error("Lỗi khi tạo bàn", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("Lỗi server: " + e.getMessage()));
    }
}

    @PostMapping("/reset/{soBan}")
    public ResponseEntity<ApiResponse<Ban>> resetBan(@PathVariable String soBan, @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        try {
            String soBanTrimmed = soBan.trim();

            Ban ban = userId != null ? banRepo.findByTaiKhoanIdAndSoBan(userId, soBanTrimmed) : banRepo.findBySoBan(soBanTrimmed);
            if (ban == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Bàn không tồn tại!"));
            }

            ban.setTrangThai(Ban.TrangThaiBan.TRONG);
            Ban banDaReset = banRepo.save(ban);
            return ResponseEntity.ok(ApiResponse.ok("Reset bàn thành công!", banDaReset));

        } catch (Exception e) {
            logger.error("Lỗi khi reset bàn", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Lỗi server: " + e.getMessage()));
        }
    }

    /**
     * Xóa bàn hoàn toàn khỏi hệ thống
     */
    @DeleteMapping("/{soBan}")
    public ResponseEntity<ApiResponse<Void>> xoaBan(@PathVariable String soBan, @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        try {
            String soBanTrimmed = soBan.trim();

            Ban ban = userId != null ? banRepo.findByTaiKhoanIdAndSoBan(userId, soBanTrimmed) : banRepo.findBySoBan(soBanTrimmed);
            if (ban == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Bàn không tồn tại!"));
            }

            if (ban.getTrangThai() != Ban.TrangThaiBan.TRONG) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.fail("Bàn đang có khách! Vui lòng thanh toán hoặc reset trước khi xóa."));
            }

            banRepo.delete(ban);
            return ResponseEntity.ok(ApiResponse.ok("Đã xóa bàn '" + soBan + "' thành công!", null));

        } catch (Exception e) {
            logger.error("Lỗi khi xóa bàn", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Lỗi server: " + e.getMessage()));
        }
    }
}