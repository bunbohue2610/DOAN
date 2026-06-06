package com.nhahang.quan.ly.mon.an.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
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

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class OrderController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 1. Lay danh sach ban
    @GetMapping("/orders/danh-sach-ban")
    public List<Map<String, Object>> getBans(@RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        if (userId == null) {
            return java.util.Collections.emptyList();
        }
        
        String sql = "SELECT b.id, b.so_ban, b.trang_thai, "
            + "COALESCE(SUM(ct.so_luong), 0) AS so_mon, "
            + "COALESCE(SUM(ct.don_gia * ct.so_luong), 0) AS tam_tinh "
            + "FROM Ban b "
            + "LEFT JOIN don_hang dh ON dh.id_ban = b.id AND dh.trang_thai_thanh_toan = 'CHUA_THANH_TOAN' "
            + "LEFT JOIN chi_tiet_don_hang ct ON ct.id_don_hang = dh.id "
            + "WHERE b.tai_khoan_id = ? "
            + "GROUP BY b.id, b.so_ban, b.trang_thai "
            + "ORDER BY b.id";
        return jdbcTemplate.queryForList(sql, userId);
    }

    // 2. Gui don (Đã tích hợp xử lý Mã Giảm Giá)
    @PostMapping("/orders/gui-don")
    public ResponseEntity<ApiResponse<Map<String, Object>>> guiDon(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @RequestBody Map<String, Object> payload) {
        try {
            String soBanStr = payload.get("soBan").toString().trim();
            double tongTien = Double.parseDouble(payload.get("tongTien").toString());
            
            String maVoucher = "";
            int phanTramGiamVoucher = 0;

            if (payload.containsKey("voucherCode") && payload.get("voucherCode") != null) {
                maVoucher = payload.get("voucherCode").toString().trim();
                if (!maVoucher.isEmpty()) {
                    maVoucher = maVoucher.toUpperCase();

                    String sqlCheckVoucher = "SELECT phan_tram_giam, so_luong, kieu_ap_dung, danh_sach_san_pham, danh_sach_danh_muc "
                        + "FROM Voucher "
                        + "WHERE ma_code = ? AND trang_thai = 1 AND (ngay_het_han IS NULL OR ngay_het_han > GETDATE()) AND so_luong > 0";

                    List<Map<String, Object>> vData = jdbcTemplate.queryForList(sqlCheckVoucher, maVoucher);
                    if (!vData.isEmpty()) {
                        phanTramGiamVoucher = ((Number) vData.get(0).get("phan_tram_giam")).intValue();
                    }
                }
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");

            String sqlFindId;
            List<Map<String, Object>> tables;
            if (userId != null) {
                sqlFindId = "SELECT id FROM Ban WHERE LTRIM(RTRIM(so_ban)) = ? AND tai_khoan_id = ?";
                tables = jdbcTemplate.queryForList(sqlFindId, soBanStr, userId);
            } else {
                sqlFindId = "SELECT id FROM Ban WHERE LTRIM(RTRIM(so_ban)) = ?";
                tables = jdbcTemplate.queryForList(sqlFindId, soBanStr);
            }

            if (tables.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Không tìm thấy bàn '" + soBanStr + "'!"));
            }
            int idBanThucTe = ((Number) tables.get(0).get("id")).intValue();

            jdbcTemplate.update("UPDATE Ban SET trang_thai = 'CO_KHACH' WHERE id = ?", idBanThucTe);

            String sqlFindOrder;
            List<Map<String, Object>> existingOrders;
            if (userId != null) {
                sqlFindOrder = "SELECT TOP 1 dh.id FROM don_hang dh "
                    + "JOIN Ban b ON dh.id_ban = b.id "
                    + "WHERE dh.id_ban = ? AND dh.trang_thai_thanh_toan = 'CHUA_THANH_TOAN' "
                    + "AND b.tai_khoan_id = ? ORDER BY dh.ngay_tao DESC";
                existingOrders = jdbcTemplate.queryForList(sqlFindOrder, idBanThucTe, userId);
            } else {
                sqlFindOrder = "SELECT TOP 1 id FROM don_hang WHERE id_ban = ? AND trang_thai_thanh_toan = 'CHUA_THANH_TOAN' ORDER BY ngay_tao DESC";
                existingOrders = jdbcTemplate.queryForList(sqlFindOrder, idBanThucTe);
            }

            long idDonHang;
            if (!existingOrders.isEmpty()) {
                idDonHang = ((Number) existingOrders.get(0).get("id")).longValue();
                jdbcTemplate.update("UPDATE don_hang SET tong_tien = tong_tien + ? WHERE id = ?", tongTien, idDonHang);

                if (phanTramGiamVoucher > 0 && maVoucher != null && !maVoucher.trim().isEmpty()) {
                    if (userId != null) {
                        jdbcTemplate.update(
                            "UPDATE don_hang SET voucher_code = ?, voucher_giam = ? WHERE id = ? AND tai_khoan_id = ? AND trang_thai_thanh_toan = 'CHUA_THANH_TOAN'",
                            maVoucher, phanTramGiamVoucher, idDonHang, userId);
                    } else {
                        jdbcTemplate.update(
                            "UPDATE don_hang SET voucher_code = ?, voucher_giam = ? WHERE id = ? AND trang_thai_thanh_toan = 'CHUA_THANH_TOAN'",
                            maVoucher, phanTramGiamVoucher, idDonHang);
                    }
                }
            } else {

                KeyHolder keyHolder = new GeneratedKeyHolder();
                String sqlInsert;
                if (userId != null) {
                    sqlInsert = "INSERT INTO don_hang (ngay_tao, tong_tien, trang_thai_don, trang_thai_thanh_toan, id_ban, tai_khoan_id, voucher_code, voucher_giam) "
                        + "VALUES (GETDATE(), ?, 'CHO_DUYET', 'CHUA_THANH_TOAN', ?, ?, ?, ?)";
                } else {
                    sqlInsert = "INSERT INTO don_hang (ngay_tao, tong_tien, trang_thai_don, trang_thai_thanh_toan, id_ban, voucher_code, voucher_giam) "
                        + "VALUES (GETDATE(), ?, 'CHO_DUYET', 'CHUA_THANH_TOAN', ?, ?, ?)";
                }
                
                final String finalMaVoucher = maVoucher;
                final int finalPhanTram = phanTramGiamVoucher;

                if (userId != null) {
                    final Integer finalUserId = userId;
                    jdbcTemplate.update(connection -> {
                        java.sql.PreparedStatement ps = connection.prepareStatement(sqlInsert, new String[]{"id"});
                        ps.setDouble(1, tongTien);
                        ps.setInt(2, idBanThucTe);
                        ps.setInt(3, finalUserId);
                        ps.setString(4, finalMaVoucher);
                        ps.setInt(5, finalPhanTram);
                        return ps;
                    }, keyHolder);
                } else {
                    jdbcTemplate.update(connection -> {
                        java.sql.PreparedStatement ps = connection.prepareStatement(sqlInsert, new String[]{"id"});
                        ps.setDouble(1, tongTien);
                        ps.setInt(2, idBanThucTe);
                        ps.setString(3, finalMaVoucher);
                        ps.setInt(4, finalPhanTram);
                        return ps;
                    }, keyHolder);
                }
                idDonHang = keyHolder.getKey().longValue();
            }

            for (Map<String, Object> item : items) {
                Object idMonAnObj = item.get("idMonAn");
                if (idMonAnObj == null) idMonAnObj = item.get("id");
                
                Object donGiaObj = item.get("giaTien");
                if (donGiaObj == null) donGiaObj = item.get("donGia");
                double donGiaGoc = Double.parseDouble(donGiaObj.toString());
                
                String sqlGetItemDiscount = "SELECT phan_tram_giam FROM mon_an WHERE id = ?";
                List<Map<String, Object>> itemData = jdbcTemplate.queryForList(sqlGetItemDiscount, idMonAnObj);
                if (!itemData.isEmpty()) {
                    Object discountObj = itemData.get(0).get("phan_tram_giam");
                    if (discountObj != null) {
                        int itemDiscount = ((Number) discountObj).intValue();
                        if (itemDiscount > 0) {
                            donGiaGoc = donGiaGoc - (donGiaGoc * itemDiscount / 100);
                        }
                    }
                }
                
                Object ghiChu = item.get("ghiChu");
                if (ghiChu == null) ghiChu = "";

                jdbcTemplate.update(
                    "INSERT INTO chi_tiet_don_hang (id_don_hang, so_luong, don_gia, id_mon_an, ghi_chu) VALUES (?, ?, ?, ?, ?)",
                    idDonHang, item.get("soLuong"), donGiaGoc, idMonAnObj, ghiChu
                );
            }

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("soBan", soBanStr);
            result.put("tongTien", tongTien);
            result.put("maVoucher", maVoucher);
            return ResponseEntity.ok(ApiResponse.ok("Gửi đơn thành công!", result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.fail("Lỗi server: " + e.getMessage()));
        }
    }

    // 3. Chi tiet ban
    @GetMapping("/orders/chi-tiet-ban/{soBan}")
    public List<Map<String, Object>> getChiTietTheoBan(
            @PathVariable String soBan,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        String sql;
        if (userId != null) {
            sql = "SELECT ct.id, ct.so_luong, ct.don_gia AS gia_tien, m.ten_mon, ct.id_mon_an, ct.ghi_chu, "
                + "m.danh_muc_id, dh.voucher_code, dh.voucher_giam "
                + "FROM chi_tiet_don_hang ct "
                + "JOIN don_hang dh ON ct.id_don_hang = dh.id "
                + "JOIN Ban b ON dh.id_ban = b.id "
                + "JOIN mon_an m ON ct.id_mon_an = m.id "
                + "WHERE b.so_ban = ? AND dh.trang_thai_thanh_toan = 'CHUA_THANH_TOAN' AND b.tai_khoan_id = ?";
            return jdbcTemplate.queryForList(sql, soBan, userId);
        } else {
            sql = "SELECT ct.id, ct.so_luong, ct.don_gia AS gia_tien, m.ten_mon, ct.id_mon_an, ct.ghi_chu, "
                + "m.danh_muc_id, dh.voucher_code, dh.voucher_giam "
                + "FROM chi_tiet_don_hang ct "
                + "JOIN don_hang dh ON ct.id_don_hang = dh.id "
                + "JOIN Ban b ON dh.id_ban = b.id "
                + "JOIN mon_an m ON ct.id_mon_an = m.id "
                + "WHERE b.so_ban = ? AND dh.trang_thai_thanh_toan = 'CHUA_THANH_TOAN'";
            return jdbcTemplate.queryForList(sql, soBan);
        }
    }

    // 4. Thanh toan + tra ve du lieu hoa don
    @PostMapping("/orders/thanh-toan/{soBan}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> thanhToan(
            @PathVariable String soBan,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @RequestBody Map<String, Object> payload) {
        try {
            String soBanTrimmed = soBan.trim();

            String sqlGetId;
            List<Map<String, Object>> orders;

            if (userId != null) {
                sqlGetId = "SELECT dh.id, dh.voucher_code, dh.voucher_giam "
                    + "FROM don_hang dh "
                    + "JOIN Ban b ON dh.id_ban = b.id "
                    + "WHERE LTRIM(RTRIM(b.so_ban)) = ? "
                    + "AND dh.trang_thai_thanh_toan = 'CHUA_THANH_TOAN' "
                    + "AND b.tai_khoan_id = ?";
                orders = jdbcTemplate.queryForList(sqlGetId, soBanTrimmed, userId);
            } else {
                sqlGetId = "SELECT dh.id, dh.voucher_code, dh.voucher_giam "
                    + "FROM don_hang dh "
                    + "JOIN Ban b ON dh.id_ban = b.id "
                    + "WHERE LTRIM(RTRIM(b.so_ban)) = ? "
                    + "AND dh.trang_thai_thanh_toan = 'CHUA_THANH_TOAN'";
                orders = jdbcTemplate.queryForList(sqlGetId, soBanTrimmed);
            }

            if (orders.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Không tìm thấy đơn hàng để thanh toán!"));
            }

            int idDon = ((Number) orders.get(0).get("id")).intValue();

            String voucherCode = null;
            int phanTramGiam = 0;

            if (orders.get(0).get("voucher_code") != null) {
                voucherCode = orders.get(0).get("voucher_code").toString();
            }
            if (orders.get(0).get("voucher_giam") != null) {
                phanTramGiam = ((Number) orders.get(0).get("voucher_giam")).intValue();
            }

            // Lay chi tiet mon de hien thi hoa don
            String sqlChiTiet = "SELECT "
                + "ct.id, "
                + "m.ten_mon, "
                + "ct.so_luong, "
                + "ct.don_gia, "
                + "(ct.so_luong * ct.don_gia) AS thanh_tien, "
                + "ct.ghi_chu "
                + "FROM chi_tiet_don_hang ct "
                + "JOIN mon_an m ON ct.id_mon_an = m.id "
                + "WHERE ct.id_don_hang = ?";

            List<Map<String, Object>> danhSachMon = jdbcTemplate.queryForList(sqlChiTiet, idDon);

            String sqlSum = "SELECT COALESCE(SUM(so_luong * don_gia), 0) AS total "
                + "FROM chi_tiet_don_hang "
                + "WHERE id_don_hang = ?";

            Double tamTinhObj = jdbcTemplate.queryForObject(sqlSum, Double.class, idDon);
            double tamTinh = tamTinhObj != null ? tamTinhObj : 0;

            double tienGiam = 0;
            double tongThanhToan = tamTinh;

            // Kiem tra voucher neu don hang co voucher
            if (voucherCode != null && !voucherCode.trim().isEmpty() && phanTramGiam > 0) {
                String ma = voucherCode.trim().toUpperCase();

                String sqlCheckVoucher = "SELECT kieu_ap_dung, danh_sach_san_pham, danh_sach_danh_muc "
                    + "FROM Voucher "
                    + "WHERE ma_code = ? "
                    + "AND trang_thai = 1 "
                    + "AND (ngay_het_han IS NULL OR ngay_het_han > GETDATE()) "
                    + "AND so_luong > 0";

                List<Map<String, Object>> voucherData = jdbcTemplate.queryForList(sqlCheckVoucher, ma);

                if (!voucherData.isEmpty()) {
                    // ✅ Kiểm tra xem sản phẩm trong đơn hàng có hợp lệ với voucher không
                    String kieuApDung = (String) voucherData.get(0).get("kieu_ap_dung");
                    boolean voucherHopLe = true;
                    
                    if (kieuApDung != null && !kieuApDung.equals("TOAN_BAN")) {
                        voucherHopLe = false; // Mặc định không hợp lệ
                        
                        if (kieuApDung.equals("CHI_DANH_MUC")) {
                            String danhSachDanhMuc = (String) voucherData.get(0).get("danh_sach_danh_muc");
                            voucherHopLe = kiemTraSanPhamVoucher_DanhMuc(jdbcTemplate, idDon, danhSachDanhMuc);
                        } else if (kieuApDung.equals("CHI_MON")) {
                            String danhSachSanPham = (String) voucherData.get(0).get("danh_sach_san_pham");
                            voucherHopLe = kiemTraSanPhamVoucher_SanPham(jdbcTemplate, idDon, danhSachSanPham);
                        }
                    }
                    
                    if (voucherHopLe) {
                        tienGiam = tamTinh * phanTramGiam / 100;
                        tongThanhToan = tamTinh - tienGiam;

                        jdbcTemplate.update(
                            "UPDATE Voucher SET so_luong = so_luong - 1 WHERE ma_code = ? AND so_luong > 0",
                            ma
                        );
                    } else {
                        // ✅ Nếu không có sản phẩm hợp lệ, bỏ voucher
                        voucherCode = null;
                        phanTramGiam = 0;
                        tienGiam = 0;
                        tongThanhToan = tamTinh;

                        jdbcTemplate.update(
                            "UPDATE don_hang SET voucher_code = NULL, voucher_giam = 0 WHERE id = ?",
                            idDon
                        );
                    }
                } else {
                    voucherCode = null;
                    phanTramGiam = 0;
                    tienGiam = 0;
                    tongThanhToan = tamTinh;

                    jdbcTemplate.update(
                        "UPDATE don_hang SET voucher_code = NULL, voucher_giam = 0 WHERE id = ?",
                        idDon
                    );
                }
            } else {
                voucherCode = null;
                phanTramGiam = 0;

                jdbcTemplate.update(
                    "UPDATE don_hang SET voucher_code = NULL, voucher_giam = 0 WHERE id = ?",
                    idDon
                );
            }

            // Cap nhat don hang da thanh toan
            jdbcTemplate.update(
                "UPDATE don_hang "
                    + "SET trang_thai_thanh_toan = 'DA_THANH_TOAN', "
                    + "trang_thai_don = 'DA_PHUC_VU', "
                    + "tong_tien = ? "
                    + "WHERE id = ?",
                tongThanhToan, idDon
            );

            // Chuyen ban ve trang thai trong
            if (userId != null) {
                jdbcTemplate.update(
                    "UPDATE Ban SET trang_thai = 'TRONG' "
                        + "WHERE LTRIM(RTRIM(so_ban)) = ? AND tai_khoan_id = ?",
                    soBanTrimmed, userId
                );
            } else {
                jdbcTemplate.update(
                    "UPDATE Ban SET trang_thai = 'TRONG' "
                        + "WHERE LTRIM(RTRIM(so_ban)) = ?",
                    soBanTrimmed
                );
            }

            // Du lieu hoa don tra ve frontend
            Map<String, Object> hoaDon = new java.util.HashMap<>();
            hoaDon.put("maDonHang", idDon);
            hoaDon.put("soBan", soBanTrimmed);
            hoaDon.put("ngayThanhToan", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            hoaDon.put("danhSachMon", danhSachMon);
            hoaDon.put("tamTinh", tamTinh);
            hoaDon.put("voucherCode", voucherCode);
            hoaDon.put("phanTramGiam", phanTramGiam);
            hoaDon.put("tienGiam", tienGiam);
            hoaDon.put("tongThanhToan", tongThanhToan);
            hoaDon.put("tongTienTraKhach", tongThanhToan);

            return ResponseEntity.ok(ApiResponse.ok("Thanh toán thành công!", hoaDon));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("Lỗi server: " + e.getMessage()));
        }
    }

    // 5. Khoi tao so do ban hang loat
    @PostMapping("/orders/khoi-tao-so-do")
    public ResponseEntity<ApiResponse<Map<String, Object>>> khoiTaoSoDo(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @RequestBody Map<String, Integer> payload) {
        try {
            int soLuong = payload.get("soLuong");
            if (userId != null) {
                jdbcTemplate.update("DELETE FROM chi_tiet_don_hang WHERE id_don_hang IN (SELECT id FROM don_hang WHERE id_ban IN (SELECT id FROM Ban WHERE tai_khoan_id = ?))", userId);
                jdbcTemplate.update("DELETE FROM don_hang WHERE id_ban IN (SELECT id FROM Ban WHERE tai_khoan_id = ?)", userId);
                jdbcTemplate.update("DELETE FROM Ban WHERE tai_khoan_id = ?", userId);
            } else {
                jdbcTemplate.update("DELETE FROM chi_tiet_don_hang");
                jdbcTemplate.update("DELETE FROM don_hang");
                jdbcTemplate.update("DELETE FROM Ban");
            }
            for (int i = 1; i <= soLuong; i++) {
                String tenBan = String.format("B%02d", i);
                if (userId != null) {
                    jdbcTemplate.update("INSERT INTO Ban (so_ban, trang_thai, tai_khoan_id) VALUES (?, 'TRONG', ?)", tenBan, userId);
                } else {
                    jdbcTemplate.update("INSERT INTO Ban (so_ban, trang_thai) VALUES (?, 'TRONG')", tenBan);
                }
            }
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("soLuong", soLuong);
            return ResponseEntity.ok(ApiResponse.ok("Khởi tạo " + soLuong + " bàn thành công!", result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.fail("Lỗi: " + e.getMessage()));
        }
    }

    // 6. Cap nhat mon tai ban khi admin them/bot mon
    @PostMapping("/orders/cap-nhat-ban/{soBan}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> capNhatBan(
            @PathVariable String soBan,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @RequestBody List<Map<String, Object>> items) {
        try {
            String sqlFind;
            List<Map<String, Object>> orders;
            if (userId != null) {
                sqlFind = "SELECT dh.id FROM don_hang dh "
                    + "JOIN Ban b ON dh.id_ban = b.id "
                    + "WHERE LTRIM(RTRIM(b.so_ban)) = ? AND dh.trang_thai_thanh_toan = 'CHUA_THANH_TOAN' AND b.tai_khoan_id = ?";
                orders = jdbcTemplate.queryForList(sqlFind, soBan.trim(), userId);
            } else {
                sqlFind = "SELECT dh.id FROM don_hang dh "
                    + "JOIN Ban b ON dh.id_ban = b.id "
                    + "WHERE LTRIM(RTRIM(b.so_ban)) = ? AND dh.trang_thai_thanh_toan = 'CHUA_THANH_TOAN'";
                orders = jdbcTemplate.queryForList(sqlFind, soBan.trim());
            }

            if (orders.isEmpty()) {
                String sqlFindBan;
                List<Map<String, Object>> bans;
                if (userId != null) {
                    sqlFindBan = "SELECT id FROM Ban WHERE LTRIM(RTRIM(so_ban)) = ? AND tai_khoan_id = ?";
                    bans = jdbcTemplate.queryForList(sqlFindBan, soBan.trim(), userId);
                } else {
                    sqlFindBan = "SELECT id FROM Ban WHERE LTRIM(RTRIM(so_ban)) = ?";
                    bans = jdbcTemplate.queryForList(sqlFindBan, soBan.trim());
                }
                if (bans.isEmpty()) return ResponseEntity.badRequest().body(ApiResponse.fail("Không tìm thấy bàn!"));

                int idBan = ((Number) bans.get(0).get("id")).intValue();
                jdbcTemplate.update("UPDATE Ban SET trang_thai = 'CO_KHACH' WHERE id = ?", idBan);

                KeyHolder keyHolder = new GeneratedKeyHolder();
                String sqlInsertDon;
                if (userId != null) {
                    sqlInsertDon = "INSERT INTO don_hang (ngay_tao, tong_tien, trang_thai_don, trang_thai_thanh_toan, id_ban, tai_khoan_id) "
                        + "VALUES (GETDATE(), 0, 'CHO_DUYET', 'CHUA_THANH_TOAN', ?, ?)";
                } else {
                    sqlInsertDon = "INSERT INTO don_hang (ngay_tao, tong_tien, trang_thai_don, trang_thai_thanh_toan, id_ban) "
                        + "VALUES (GETDATE(), 0, 'CHO_DUYET', 'CHUA_THANH_TOAN', ?)";
                }

                if (userId != null) {
                    final Integer finalUserId = userId;
                    jdbcTemplate.update(conn -> {
                        java.sql.PreparedStatement ps = conn.prepareStatement(sqlInsertDon, new String[]{"id"});
                        ps.setInt(1, idBan);
                        ps.setInt(2, finalUserId);
                        return ps;
                    }, keyHolder);
                } else {
                    jdbcTemplate.update(conn -> {
                        java.sql.PreparedStatement ps = conn.prepareStatement(sqlInsertDon, new String[]{"id"});
                        ps.setInt(1, idBan);
                        return ps;
                    }, keyHolder);
                }

                long idDon = keyHolder.getKey().longValue();
                for (Map<String, Object> item : items) {
                    jdbcTemplate.update(
                        "INSERT INTO chi_tiet_don_hang (id_don_hang, so_luong, don_gia, id_mon_an) VALUES (?, ?, ?, ?)",
                        idDon, item.get("so_luong"), item.get("gia_tien"), item.get("mon_an_id")
                    );
                }
                jdbcTemplate.update(
                    "UPDATE don_hang SET tong_tien = (SELECT COALESCE(SUM(so_luong * don_gia), 0) FROM chi_tiet_don_hang WHERE id_don_hang = ?) WHERE id = ?",
                    idDon, idDon
                );
            } else {
                long idDon = ((Number) orders.get(0).get("id")).longValue();
                jdbcTemplate.update("DELETE FROM chi_tiet_don_hang WHERE id_don_hang = ?", idDon);
                for (Map<String, Object> item : items) {
                    jdbcTemplate.update(
                        "INSERT INTO chi_tiet_don_hang (id_don_hang, so_luong, don_gia, id_mon_an) VALUES (?, ?, ?, ?)",
                        idDon, item.get("so_luong"), item.get("gia_tien"), item.get("mon_an_id")
                    );
                }
                jdbcTemplate.update(
                    "UPDATE don_hang SET tong_tien = (SELECT COALESCE(SUM(so_luong * don_gia), 0) FROM chi_tiet_don_hang WHERE id_don_hang = ?) WHERE id = ?",
                    idDon, idDon
                );
            }
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("soBan", soBan.trim());
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật bàn thành công!", result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.fail("Lỗi: " + e.getMessage()));
        }
    }

    // 7. Xoa ban
    @DeleteMapping("/orders/xoa-ban/{soBan}")
    public ResponseEntity<ApiResponse<Void>> xoaBan(
            @PathVariable String soBan,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        try {
            soBan = soBan.trim();

            String sqlCheckBan;
            List<Map<String, Object>> bans;
            if (userId != null) {
                sqlCheckBan = "SELECT id FROM Ban WHERE LTRIM(RTRIM(so_ban)) = ? AND tai_khoan_id = ?";
                bans = jdbcTemplate.queryForList(sqlCheckBan, soBan, userId);
            } else {
                sqlCheckBan = "SELECT id FROM Ban WHERE LTRIM(RTRIM(so_ban)) = ?";
                bans = jdbcTemplate.queryForList(sqlCheckBan, soBan);
            }

            if (bans.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Không tìm thấy bàn " + soBan));
            }

            int idBan = ((Number) bans.get(0).get("id")).intValue();

            jdbcTemplate.update("DELETE FROM chi_tiet_don_hang WHERE id_don_hang IN (SELECT id FROM don_hang WHERE id_ban = ?)", idBan);
            jdbcTemplate.update("DELETE FROM don_hang WHERE id_ban = ?", idBan);
            jdbcTemplate.update("DELETE FROM Ban WHERE id = ?", idBan);

            return ResponseEntity.ok(ApiResponse.ok("Đã xóa bàn " + soBan + " thành công!", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.fail("Lỗi server: " + e.getMessage()));
        }
    }

    // 8. Thong ke hom nay
    @GetMapping("/orders/thong-ke-hom-nay")
    public Map<String, Object> thongKeHomNay(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            Double doanhThu;
            Integer soDon;
            if (userId != null) {
                doanhThu = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(tong_tien), 0) FROM don_hang "
                    + "WHERE trang_thai_thanh_toan = 'DA_THANH_TOAN' "
                    + "AND CAST(ngay_tao AS DATE) = CAST(GETDATE() AS DATE) "
                    + "AND tai_khoan_id = ?",
                    Double.class, userId);
                soDon = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM don_hang "
                    + "WHERE trang_thai_thanh_toan = 'DA_THANH_TOAN' "
                    + "AND CAST(ngay_tao AS DATE) = CAST(GETDATE() AS DATE) "
                    + "AND tai_khoan_id = ?",
                    Integer.class, userId);
            } else {
                doanhThu = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(tong_tien), 0) FROM don_hang "
                    + "WHERE trang_thai_thanh_toan = 'DA_THANH_TOAN' "
                    + "AND CAST(ngay_tao AS DATE) = CAST(GETDATE() AS DATE)",
                    Double.class);
                soDon = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM don_hang "
                    + "WHERE trang_thai_thanh_toan = 'DA_THANH_TOAN' "
                    + "AND CAST(ngay_tao AS DATE) = CAST(GETDATE() AS DATE)",
                    Integer.class);
            }
            result.put("doanhThu", doanhThu != null ? doanhThu : 0);
            result.put("soDon", soDon != null ? soDon : 0);
        } catch (Exception e) {
            result.put("doanhThu", 0);
            result.put("soDon", 0);
            result.put("error", e.getMessage());
        }
        return result;
    }

    // 9. Reset ban
    @PostMapping("/orders/reset-ban/{soBan}")
    public ResponseEntity<ApiResponse<Void>> resetBan(
            @PathVariable String soBan,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        try {
            String soBanTrimmed = soBan.trim();

            String sqlFind;
            List<Map<String, Object>> orders;
            if (userId != null) {
                sqlFind = "SELECT dh.id FROM don_hang dh JOIN Ban b ON dh.id_ban = b.id "
                    + "WHERE LTRIM(RTRIM(b.so_ban)) = ? "
                    + "AND dh.trang_thai_thanh_toan = 'CHUA_THANH_TOAN' "
                    + "AND b.tai_khoan_id = ?";
                orders = jdbcTemplate.queryForList(sqlFind, soBanTrimmed, userId);
            } else {
                sqlFind = "SELECT dh.id FROM don_hang dh JOIN Ban b ON dh.id_ban = b.id "
                    + "WHERE LTRIM(RTRIM(b.so_ban)) = ? "
                    + "AND dh.trang_thai_thanh_toan = 'CHUA_THANH_TOAN'";
                orders = jdbcTemplate.queryForList(sqlFind, soBanTrimmed);
            }

            if (!orders.isEmpty()) {
                int idDon = ((Number) orders.get(0).get("id")).intValue();
                jdbcTemplate.update("DELETE FROM chi_tiet_don_hang WHERE id_don_hang = ?", idDon);
                jdbcTemplate.update("DELETE FROM don_hang WHERE id = ?", idDon);
            }

            jdbcTemplate.update(
                "UPDATE Ban SET trang_thai = 'TRONG' WHERE LTRIM(RTRIM(so_ban)) = ?",
                soBanTrimmed
            );

            return ResponseEntity.ok(ApiResponse.ok("Reset bàn thành công!", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("Lỗi: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 10. Khách gọi nhân viên
    // ─────────────────────────────────────────────────────────────────
    @PostMapping("/orders/goi-nhan-vien")
    public ResponseEntity<ApiResponse<Void>> goiNhanVien(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId,
            @RequestBody Map<String, Object> payload) {
        try {
            String soBan = payload.getOrDefault("soBan", "").toString().trim();
            if (soBan.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.fail("Thiếu số bàn!"));
            }

            // Tạo bảng nếu chưa tồn tại
            jdbcTemplate.execute(
                "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='yeu_cau_nhan_vien' AND xtype='U') " +
                "CREATE TABLE yeu_cau_nhan_vien (" +
                "  id            INT IDENTITY(1,1) PRIMARY KEY," +
                "  so_ban        NVARCHAR(50) NOT NULL," +
                "  tai_khoan_id  INT," +
                "  da_xu_ly      BIT          NOT NULL DEFAULT 0," +
                "  ngay_tao      DATETIME     NOT NULL DEFAULT GETDATE()" +
                ")"
            );

            jdbcTemplate.update(
                "INSERT INTO yeu_cau_nhan_vien (so_ban, tai_khoan_id) VALUES (?, ?)",
                soBan, userId
            );

            return ResponseEntity.ok(ApiResponse.ok("Đã gọi nhân viên!", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("Lỗi: " + e.getMessage()));
        }
    }

    // 11. Admin lấy danh sách yêu cầu gọi nhân viên chưa xử lý
    @GetMapping("/orders/yeu-cau-nhan-vien")
    public ResponseEntity<List<Map<String, Object>>> layYeuCauNhanVien(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        try {
            jdbcTemplate.execute(
                "IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='yeu_cau_nhan_vien' AND xtype='U') " +
                "CREATE TABLE yeu_cau_nhan_vien (" +
                "  id            INT IDENTITY(1,1) PRIMARY KEY," +
                "  so_ban        NVARCHAR(50) NOT NULL," +
                "  tai_khoan_id  INT," +
                "  da_xu_ly      BIT          NOT NULL DEFAULT 0," +
                "  ngay_tao      DATETIME     NOT NULL DEFAULT GETDATE()" +
                ")"
            );

            String sql;
            List<Map<String, Object>> rows;
            if (userId != null) {
                sql = "SELECT id, so_ban, ngay_tao FROM yeu_cau_nhan_vien " +
                      "WHERE da_xu_ly = 0 AND tai_khoan_id = ? ORDER BY ngay_tao ASC";
                rows = jdbcTemplate.queryForList(sql, userId);
            } else {
                sql = "SELECT id, so_ban, ngay_tao FROM yeu_cau_nhan_vien " +
                      "WHERE da_xu_ly = 0 ORDER BY ngay_tao ASC";
                rows = jdbcTemplate.queryForList(sql);
            }
            return ResponseEntity.ok(rows);
        } catch (Exception e) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    // 12. Admin xác nhận đã xử lý yêu cầu
    @PostMapping("/orders/xu-ly-nhan-vien/{id}")
    public ResponseEntity<ApiResponse<Void>> xuLyNhanVien(@PathVariable int id) {
        try {
            jdbcTemplate.update(
                "UPDATE yeu_cau_nhan_vien SET da_xu_ly = 1 WHERE id = ?", id
            );
            return ResponseEntity.ok(ApiResponse.ok("Đã xử lý!", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("Lỗi: " + e.getMessage()));
        }
    }

    // ✅ Helper method: Kiểm tra xem có sản phẩm nào trong đơn hàng nằm trong danh sách danh mục của voucher không
    private boolean kiemTraSanPhamVoucher_DanhMuc(JdbcTemplate jdbcTemplate, int idDon, String danhSachDanhMuc) {
        if (danhSachDanhMuc == null || danhSachDanhMuc.trim().isEmpty()) {
            return false;
        }
        try {
            java.util.List<Integer> danhMucIds = parseJsonArray(danhSachDanhMuc);
            if (danhMucIds == null || danhMucIds.isEmpty()) {
                return false;
            }
            
            String sql = "SELECT COUNT(*) AS cnt FROM chi_tiet_don_hang ct "
                + "JOIN mon_an m ON ct.id_mon_an = m.id "
                + "WHERE ct.id_don_hang = ? AND m.danh_muc_id IN (" 
                + String.join(",", danhMucIds.stream().map(String::valueOf).toArray(String[]::new)) + ")";
            
            Integer cnt = jdbcTemplate.queryForObject(sql, Integer.class, idDon);
            return cnt != null && cnt > 0;
        } catch (Exception e) {
            System.err.println("Lỗi kiểm tra danh mục voucher: " + e.getMessage());
            return false;
        }
    }

    // ✅ Helper method: Kiểm tra xem có sản phẩm nào trong đơn hàng nằm trong danh sách sản phẩm của voucher không
    private boolean kiemTraSanPhamVoucher_SanPham(JdbcTemplate jdbcTemplate, int idDon, String danhSachSanPham) {
        if (danhSachSanPham == null || danhSachSanPham.trim().isEmpty()) {
            return false;
        }
        try {
            java.util.List<Integer> sanPhamIds = parseJsonArray(danhSachSanPham);
            if (sanPhamIds == null || sanPhamIds.isEmpty()) {
                return false;
            }
            
            String sql = "SELECT COUNT(*) AS cnt FROM chi_tiet_don_hang ct "
                + "WHERE ct.id_don_hang = ? AND ct.id_mon_an IN (" 
                + String.join(",", sanPhamIds.stream().map(String::valueOf).toArray(String[]::new)) + ")";
            
            Integer cnt = jdbcTemplate.queryForObject(sql, Integer.class, idDon);
            return cnt != null && cnt > 0;
        } catch (Exception e) {
            System.err.println("Lỗi kiểm tra sản phẩm voucher: " + e.getMessage());
            return false;
        }
    }

    // ✅ Helper method: Parse JSON array string thành List<Integer>
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
            System.err.println("Lỗi parse JSON array: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
}