package com.nhahang.quan.ly.mon.an.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhahang.quan.ly.mon.an.dto.ApiResponse;
import com.nhahang.quan.ly.mon.an.entity.TaiKhoan;
import com.nhahang.quan.ly.mon.an.entity.Voucher;
import com.nhahang.quan.ly.mon.an.repository.TaiKhoanRepository;
import com.nhahang.quan.ly.mon.an.repository.VoucherRepository;

@RestController
@RequestMapping("/api/voucher")
@CrossOrigin("*")
public class VoucherController {
    
    private static final Logger logger = LoggerFactory.getLogger(VoucherController.class);

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    // ✅ Lấy admin ID từ header X-User-Id
    private Integer getAdminIdFromHeader(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(userIdHeader.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid user ID format: {}", userIdHeader);
            return null;
        }
    }

    // 1. Lấy danh sách Voucher cho trang Admin (chỉ voucher của admin đó)
    @GetMapping
    public ResponseEntity<?> getAllVouchers(@RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Integer adminId = getAdminIdFromHeader(userIdHeader);
        
        if (adminId == null) {
            logger.warn("No valid user ID provided");
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "User ID không hợp lệ", null));
        }
        
        List<Voucher> vouchers = voucherRepository.findByTaiKhoanId(adminId);
        return ResponseEntity.ok(vouchers);
    }

    // 2. Thêm hoặc Sửa Voucher
    @PostMapping
    public ResponseEntity<?> saveVoucher(@RequestBody Voucher voucher, 
                                         @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        try {
            // Validate input
            if (voucher.getMaCode() == null || voucher.getMaCode().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Mã code không được để trống", null));
            }
            
            // Lấy admin ID
            Integer adminId = getAdminIdFromHeader(userIdHeader);
            if (adminId == null) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "User ID không hợp lệ", null));
            }
            
            // Uppercase mã code
            voucher.setMaCode(voucher.getMaCode().trim().toUpperCase());
            
            // Đảm bảo kieuApDung có giá trị
            if (voucher.getKieuApDung() == null || voucher.getKieuApDung().trim().isEmpty()) {
                voucher.setKieuApDung("TOAN_BAN");
            }
            
            // Đảm bảo trangThai có giá trị
            if (voucher.getTrangThai() == null) {
                voucher.setTrangThai(true);
            }
            
            // Đảm bảo soLuong có giá trị
            if (voucher.getSoLuong() == null || voucher.getSoLuong() <= 0) {
                voucher.setSoLuong(100);
            }
            
            // Validate phanTramGiam
            if (voucher.getPhanTramGiam() == null || voucher.getPhanTramGiam() <= 0 || voucher.getPhanTramGiam() > 100) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Phần trăm giảm giá phải từ 1 đến 100", null));
            }
            
            // ✅ Thiết lập admin (TaiKhoan) cho voucher
            if (voucher.getId() == null) {
                // Nếu là tạo mới, gán admin từ header
                Optional<TaiKhoan> adminOpt = taiKhoanRepository.findById(adminId);
                if (adminOpt.isPresent()) {
                    voucher.setTaiKhoan(adminOpt.get());
                    logger.info("Creating new voucher for admin ID: {}", adminId);
                } else {
                    return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Admin không tồn tại", null));
                }
            } else {
                // Nếu là cập nhật, kiểm tra voucher có thuộc admin này không
                Optional<Voucher> existingVoucher = voucherRepository.findById(voucher.getId());
                if (existingVoucher.isPresent()) {
                    Voucher existing = existingVoucher.get();
                    if (existing.getTaiKhoan() == null || !existing.getTaiKhoan().getId().equals(adminId)) {
                        logger.warn("Admin {} tried to edit voucher belonging to another admin", adminId);
                        return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Voucher này không thuộc về bạn", null));
                    }
                    // Giữ nguyên admin của voucher cũ
                    voucher.setTaiKhoan(existing.getTaiKhoan());
                } else {
                    return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Voucher không tồn tại", null));
                }
            }
            
            logger.info("Attempting to save voucher: {} for admin ID: {}", voucher.getMaCode(), adminId);
            Voucher saved = voucherRepository.save(voucher);
            logger.info("Voucher saved successfully: ID={}, Code={}, AdminID={}", saved.getId(), saved.getMaCode(), adminId);
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Lưu mã giảm giá thành công!", saved));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            logger.error("Database constraint violation when saving voucher", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Mã code này đã tồn tại cho tài khoản này, vui lòng chọn mã khác", null));
        } catch (Exception e) {
            logger.error("Error saving voucher", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Lỗi khi lưu voucher: " + e.getMessage(), null));
        }
    }

    // 3. Xóa Voucher
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVoucher(@PathVariable Integer id,
                                          @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        Integer adminId = getAdminIdFromHeader(userIdHeader);
        if (adminId == null) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "User ID không hợp lệ", null));
        }
        
        Optional<Voucher> voucherOpt = voucherRepository.findById(id);
        if (!voucherOpt.isPresent()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Mã không tồn tại", null));
        }
        
        // ✅ Kiểm tra voucher có thuộc admin này không
        Voucher voucher = voucherOpt.get();
        if (voucher.getTaiKhoan() == null || !voucher.getTaiKhoan().getId().equals(adminId)) {
            logger.warn("Admin {} tried to delete voucher belonging to another admin", adminId);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Voucher này không thuộc về bạn", null));
        }
        
        voucherRepository.deleteById(id);
        logger.info("Voucher deleted: ID={} by admin ID={}", id, adminId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Đã xóa mã giảm giá", null));
    }

    // 4. API cho Khách hàng kiểm tra mã giảm giá trên Menu (không cần admin ID)
    @GetMapping("/check")
    public ResponseEntity<?> checkVoucher(
            @RequestParam String code,
            @RequestParam(required = false) String productIds,
            @RequestParam(required = false) String categoryIds) {
        try {
            logger.info("Voucher check: code={}, productIds={}, categoryIds={}", code, productIds, categoryIds);
            
            Optional<Voucher> v = voucherRepository.findByMaCodeAndTrangThai(code.trim().toUpperCase(), true);
            if (!v.isPresent()) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Mã giảm giá không hợp lệ!", null));
            }
            
            Voucher voucher = v.get();
            
            // Kiểm tra ngày hết hạn
            if (voucher.getNgayHetHan() != null && voucher.getNgayHetHan().before(new Date())) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Mã giảm giá đã hết hạn!", null));
            }
            
            // Kiểm tra số lượng
            if (voucher.getSoLuong() != null && voucher.getSoLuong() <= 0) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Mã giảm giá đã hết lượt sử dụng!", null));
            }
            
            // ✅ Kiểm tra xem voucher có áp dụng cho sản phẩm/danh mục trong giỏ không
            String kieuApDung = voucher.getKieuApDung();
            if (kieuApDung != null && !kieuApDung.equals("TOAN_BAN")) {
                boolean hasValidProduct = false;
                
                if (kieuApDung.equals("CHI_DANH_MUC")) {
                    // Kiểm tra xem có danh mục nào hợp lệ trong giỏ
                    if (categoryIds != null && !categoryIds.isEmpty()) {
                        try {
                            java.util.List<Integer> cartCategoryIds = parseJsonArray(categoryIds);
                            java.util.List<Integer> voucherCategoryIds = parseJsonArray(voucher.getDanhSachDanhMuc());
                            
                            if (voucherCategoryIds != null) {
                                for (Integer cartCatId : cartCategoryIds) {
                                    if (voucherCategoryIds.contains(cartCatId)) {
                                        hasValidProduct = true;
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Lỗi kiểm tra danh mục voucher", e);
                        }
                    }
                } else if (kieuApDung.equals("CHI_MON")) {
                    // Kiểm tra xem có sản phẩm nào hợp lệ trong giỏ
                    if (productIds != null && !productIds.isEmpty()) {
                        try {
                            java.util.List<Integer> cartProductIds = parseJsonArray(productIds);
                            java.util.List<Integer> voucherProductIds = parseJsonArray(voucher.getDanhSachSanPham());
                            
                            if (voucherProductIds != null) {
                                for (Integer cartProdId : cartProductIds) {
                                    if (voucherProductIds.contains(cartProdId)) {
                                        hasValidProduct = true;
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Lỗi kiểm tra sản phẩm voucher", e);
                        }
                    }
                }
                
                // ✅ Nếu kiểu áp dụng hạn chế nhưng không có sản phẩm hợp lệ
                if (!hasValidProduct) {
                    if (kieuApDung.equals("CHI_DANH_MUC")) {
                        return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Mã giảm giá không áp dụng cho danh mục sản phẩm này!", null));
                    } else if (kieuApDung.equals("CHI_MON")) {
                        return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Mã giảm giá không áp dụng cho sản phẩm này!", null));
                    }
                }
            }
            
            return ResponseEntity.ok(voucher); // Trả về thông tin voucher hợp lệ
        } catch (Exception e) {
            logger.error("Lỗi kiểm tra voucher:", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Lỗi kiểm tra mã giảm giá!", null));
        }
    }
    
    // ✅ Helper method: parse JSON array string thành List<Integer>
    private java.util.List<Integer> parseJsonArray(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        try {
            // Remove brackets and split by comma
            String content = json.trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                content = content.substring(1, content.length() - 1);
            }
            java.util.List<Integer> result = new java.util.ArrayList<>();
            if (!content.isEmpty()) {
                for (String id : content.split(",")) {
                    try {
                        result.add(Integer.parseInt(id.trim()));
                    } catch (NumberFormatException e) {
                        // Skip invalid numbers
                    }
                }
            }
            return result;
        } catch (Exception e) {
            logger.warn("Lỗi parse JSON array: {}", json);
            return new java.util.ArrayList<>();
        }
    }
}