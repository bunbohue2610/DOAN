package com.nhahang.quan.ly.mon.an.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhahang.quan.ly.mon.an.dto.ApiResponse;
import com.nhahang.quan.ly.mon.an.entity.DanhMuc;
import com.nhahang.quan.ly.mon.an.entity.TaiKhoan;
import com.nhahang.quan.ly.mon.an.repository.DanhMucRepository;
import com.nhahang.quan.ly.mon.an.repository.TaiKhoanRepository;

@RestController
@RequestMapping("/api/danh-muc")
@CrossOrigin("*")
public class DanhMucController {

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    // Inject thêm JdbcTemplate để update nhanh toàn bộ món ăn trong danh mục
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 1. LẤY DANH SÁCH DANH MỤC CỦA NGƯỜI DÙNG HIỆN TẠI
    @GetMapping
    public List<DanhMuc> layTatCa(@RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        if (userId != null) {
            return danhMucRepository.findByTaiKhoanId(userId);
        }
        return java.util.Collections.emptyList();
    }

    // 1.1 [MỚI] LẤY DANH SÁCH TẤT CẢ DANH MỤC (cho Admin khi tạo Voucher)
    @GetMapping("/all")
    public List<DanhMuc> layTatCaDanhMuc(@RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        // Nếu có userId, lấy danh mục của user đó
        if (userId != null) {
            return danhMucRepository.findByTaiKhoanId(userId);
        }
        // Nếu không có userId, trả về tất cả (cho admin xem tất cả)
        return danhMucRepository.findAll();
    }

    // 2. TẠO DANH MỤC MỚI
    @PostMapping
    public ResponseEntity<?> themDanhMuc(@RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-User-Id", required = true) Integer userId) {
        String tenDanhMuc = payload.get("tenDanhMuc");

        // Kiểm tra dữ liệu đầu vào
        if (tenDanhMuc == null || tenDanhMuc.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Tên danh mục không được để trống", null));
        }

        // Kiểm tra xem danh mục đã tồn tại chưa (cho user này)
        if (danhMucRepository.existsByTaiKhoanIdAndTenDanhMuc(userId, tenDanhMuc)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Danh mục này đã tồn tại", null));
        }

        // Lấy thông tin tài khoản
        Optional<TaiKhoan> userOpt = taiKhoanRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Tài khoản không hợp lệ", null));
        }

        // Tạo danh mục mới
        DanhMuc danhMuc = new DanhMuc();
        danhMuc.setTenDanhMuc(tenDanhMuc.trim());
        danhMuc.setTaiKhoan(userOpt.get());

        danhMucRepository.save(danhMuc);

        Map<String, Object> data = new HashMap<>();
        data.put("id", danhMuc.getId());
        data.put("tenDanhMuc", danhMuc.getTenDanhMuc());

        return ResponseEntity.ok(new ApiResponse<>(true, "Thêm danh mục thành công", data));
    }

    // 3. CẬP NHẬT DANH MỤC
    @PutMapping("/{id}")
    public ResponseEntity<?> capNhatDanhMuc(@PathVariable Integer id, @RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-User-Id", required = true) Integer userId) {
        String tenDanhMuc = payload.get("tenDanhMuc");

        if (tenDanhMuc == null || tenDanhMuc.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Tên danh mục không được để trống", null));
        }

        Optional<DanhMuc> danhMucOpt = danhMucRepository.findById(id);
        if (!danhMucOpt.isPresent()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Danh mục không tồn tại", null));
        }

        DanhMuc danhMuc = danhMucOpt.get();

        // Kiểm tra xem danh mục này có thuộc về user hiện tại không
        if (!danhMuc.getTaiKhoan().getId().equals(userId)) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse<>(false, "Bạn không có quyền sửa danh mục này", null));
        }

        danhMuc.setTenDanhMuc(tenDanhMuc.trim());
        danhMucRepository.save(danhMuc);

        Map<String, Object> data = new HashMap<>();
        data.put("id", danhMuc.getId());
        data.put("tenDanhMuc", danhMuc.getTenDanhMuc());

        return ResponseEntity.ok(new ApiResponse<>(true, "Cập nhật danh mục thành công", data));
    }

    // 4. XÓA DANH MỤC
    @DeleteMapping("/{id}")
    public ResponseEntity<?> xoaDanhMuc(@PathVariable Integer id,
            @RequestHeader(value = "X-User-Id", required = true) Integer userId) {
        Optional<DanhMuc> danhMucOpt = danhMucRepository.findById(id);
        if (!danhMucOpt.isPresent()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Danh mục không tồn tại", null));
        }

        DanhMuc danhMuc = danhMucOpt.get();

        // Kiểm tra xem danh mục này có thuộc về user hiện tại không
        if (!danhMuc.getTaiKhoan().getId().equals(userId)) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse<>(false, "Bạn không có quyền xóa danh mục này", null));
        }

        danhMucRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Xóa danh mục thành công", null));
    }

    // 5. [MỚI] CẬP NHẬT PHẦN TRĂM GIẢM GIÁ CHO DANH MỤC VÀ MÓN ĂN
    @PutMapping("/{id}/giam-gia")
    public ResponseEntity<?> capNhatGiamGia(@PathVariable Integer id, @RequestBody Map<String, Integer> payload,
            @RequestHeader(value = "X-User-Id", required = true) Integer userId) {
        
        Integer phanTramGiam = payload.get("phanTramGiam");
        
        if (phanTramGiam == null || phanTramGiam < 0 || phanTramGiam > 100) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Phần trăm giảm giá không hợp lệ (0-100)", null));
        }

        Optional<DanhMuc> danhMucOpt = danhMucRepository.findById(id);
        if (!danhMucOpt.isPresent()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Danh mục không tồn tại", null));
        }

        DanhMuc danhMuc = danhMucOpt.get();

        // Kiểm tra quyền sở hữu
        if (!danhMuc.getTaiKhoan().getId().equals(userId)) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse<>(false, "Bạn không có quyền thao tác trên danh mục này", null));
        }

        // Cập nhật phần trăm giảm giá cho danh mục
        danhMuc.setPhanTramGiam(phanTramGiam);
        danhMucRepository.save(danhMuc);

        // Đồng bộ phần trăm giảm giá cho tất cả các món ăn thuộc danh mục này
        jdbcTemplate.update("UPDATE MonAn SET phan_tram_giam = ? WHERE danh_muc_id = ?", phanTramGiam, id);

        Map<String, Object> data = new HashMap<>();
        data.put("id", danhMuc.getId());
        data.put("tenDanhMuc", danhMuc.getTenDanhMuc());
        data.put("phanTramGiam", danhMuc.getPhanTramGiam());

        return ResponseEntity.ok(new ApiResponse<>(true, "Đã áp dụng giảm giá " + phanTramGiam + "% cho toàn bộ món thuộc danh mục này", data));
    }
}