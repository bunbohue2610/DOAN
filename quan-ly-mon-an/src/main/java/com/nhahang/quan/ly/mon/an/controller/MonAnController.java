package com.nhahang.quan.ly.mon.an.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nhahang.quan.ly.mon.an.dto.ApiResponse;
import com.nhahang.quan.ly.mon.an.entity.DanhMuc;
import com.nhahang.quan.ly.mon.an.entity.MonAn;
import com.nhahang.quan.ly.mon.an.entity.TaiKhoan;
import com.nhahang.quan.ly.mon.an.repository.ChiTietDonHangRepository;
import com.nhahang.quan.ly.mon.an.repository.MonAnRepository;
import com.nhahang.quan.ly.mon.an.repository.TaiKhoanRepository;

@RestController
@RequestMapping("/api/mon-an")
@CrossOrigin("*")
public class MonAnController {

    @Autowired
    private MonAnRepository monAnRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private ChiTietDonHangRepository chiTietDonHangRepository;

    // Lấy toàn bộ món ăn của tài khoản hiện tại
    @GetMapping
    public List<MonAn> getAll(@RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        if (userId != null) {
            return monAnRepository.findByTaiKhoanId(userId);
        }
        return java.util.Collections.emptyList();
    }

    // [MỚI] Lấy tất cả món ăn (cho Admin khi tạo Voucher)
    @GetMapping("/all")
    public List<MonAn> getAllMonAn(@RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        // Nếu có userId, lấy món ăn của user đó
        if (userId != null) {
            return monAnRepository.findByTaiKhoanId(userId);
        }
        // Nếu không có userId, trả về tất cả
        return monAnRepository.findAll();
    }

    // Tìm kiếm món ăn theo tên của tài khoản
    @GetMapping("/search")
    public List<MonAn> search(@RequestParam String kw,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        if (userId != null) {
            return monAnRepository.timKiemTheoTen(userId, kw);
        }
        return java.util.Collections.emptyList();
    }

    // ✅ THÊM MỚI
    @PostMapping
    public ResponseEntity<?> create(@RequestBody MonAn monAn,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        if (userId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        TaiKhoan taiKhoan = taiKhoanRepository.findById(userId).orElse(null);
        if (taiKhoan == null)
            return ResponseEntity.badRequest().body(ApiResponse.fail("Tài khoản không tồn tại"));

        monAn.setId(null); // Bắt buộc! Tránh ghi đè món cũ
        monAn.setTaiKhoan(taiKhoan);
        MonAn saved = monAnRepository.save(monAn);
        return ResponseEntity.ok(ApiResponse.ok("Thêm món ăn thành công!", saved));
    }

    // ✅ CẬP NHẬT
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id,
            @RequestBody MonAn monAnMoi,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        if (userId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        MonAn monAnCu = monAnRepository.findById(id).orElse(null);
        if (monAnCu == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("Món ăn không tồn tại"));

        // Kiểm tra quyền sở hữu (chỉ kiểm tra nếu taiKhoan không null)
        if (monAnCu.getTaiKhoan() != null && !monAnCu.getTaiKhoan().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail("Không có quyền sửa món này!"));
        }

        // Chỉ update các field được phép, GIỮ NGUYÊN taiKhoan
        monAnCu.setTenMon(monAnMoi.getTenMon());
        monAnCu.setGiaTien(monAnMoi.getGiaTien());
        monAnCu.setMoTa(monAnMoi.getMoTa());
        if (monAnMoi.getHinhAnh() != null) {
            monAnCu.setHinhAnh(monAnMoi.getHinhAnh());
        }

        // Chỉ cập nhật danh mục nếu có id hợp lệ, giữ nguyên nếu null
        DanhMuc danhMucMoi = monAnMoi.getDanhMuc();
        if (danhMucMoi != null && danhMucMoi.getId() != null) {
            monAnCu.setDanhMuc(danhMucMoi);
        }

        MonAn updated = monAnRepository.save(monAnCu);
        return ResponseEntity.ok(ApiResponse.ok("Sửa món ăn thành công!", updated));
    }

    // ✅ XÓA MÓN ĂN - xóa cả các chi tiết đơn hàng liên kết
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable Integer id,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        try {
            // Tìm món trước để kiểm tra
            MonAn monAn = monAnRepository.findById(id).orElse(null);

            if (monAn == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Lỗi: Món ăn với ID " + id + " không tồn tại!"));
            }

            // ✅ FIX: Kiểm tra null taiKhoan trước khi gọi getId()
            // Các món cũ thêm trước khi có multi-user sẽ có taiKhoan = null → bỏ qua kiểm tra quyền
            if (userId != null && monAn.getTaiKhoan() != null) {
                if (!monAn.getTaiKhoan().getId().equals(userId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(ApiResponse.fail("Bạn không có quyền xóa món ăn này!"));
                }
            }

            // ✅ Xóa tất cả chi tiết đơn hàng liên kết với món này trước
            chiTietDonHangRepository.deleteByMonAnId(id);
            
            // Sau đó xóa món ăn
            monAnRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.ok("Đã xóa món ăn thành công!", ""));

        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            String errorMsg;
            if (msg.contains("foreign key") || msg.contains("constraint") || msg.contains("constraintviolation")) {
                errorMsg = "Không thể xóa! Lỗi ràng buộc dữ liệu.";
            } else {
                errorMsg = "Không thể xóa món ăn này. Chi tiết: " + e.getMessage();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(errorMsg));
        }
    }

    @PostMapping("/upload")
    public String uploadAnh(@RequestParam("file") MultipartFile file) {
        try {
            Path uploadPath = Paths.get("uploads");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String tenFileGoc = file.getOriginalFilename();
            String tenFileMoi = UUID.randomUUID().toString() + "_" + tenFileGoc;

            Path duongDanFile = uploadPath.resolve(tenFileMoi);
            Files.copy(file.getInputStream(), duongDanFile, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + tenFileMoi;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}