package com.nhahang.quan.ly.mon.an.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nhahang.quan.ly.mon.an.dto.ThongKeHomNayDTO;
import com.nhahang.quan.ly.mon.an.dto.ThongKeNgayDTO;
import com.nhahang.quan.ly.mon.an.dto.ThongKeQuyDTO;
import com.nhahang.quan.ly.mon.an.dto.ThongKeThangDTO;
import com.nhahang.quan.ly.mon.an.dto.TopMonDTO;
import com.nhahang.quan.ly.mon.an.repository.DonHangRepository;

@RestController
@RequestMapping("/api/thong-ke")
@CrossOrigin(origins = "*")
public class ThongKeController {

    @Autowired
    private DonHangRepository donHangRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Thống kê hôm nay
     */
    @GetMapping("/hom-nay")
    public ResponseEntity<ThongKeHomNayDTO> thongKeHomNay(
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {

        Map<String, Object> result;

        if (userId != null) {
            String sql = "SELECT COALESCE(SUM(tong_tien), 0) as doanhThu, COUNT(id) as soDon " +
                         "FROM don_hang " +
                         "WHERE CAST(ngay_tao AS DATE) = CAST(GETDATE() AS DATE) " +
                         "AND trang_thai_thanh_toan = 'DA_THANH_TOAN' " +
                         "AND tai_khoan_id = ?";
            result = jdbcTemplate.queryForMap(sql, userId);
        } else {
            String sql = "SELECT COALESCE(SUM(tong_tien), 0) as doanhThu, COUNT(id) as soDon " +
                         "FROM don_hang " +
                         "WHERE CAST(ngay_tao AS DATE) = CAST(GETDATE() AS DATE) " +
                         "AND trang_thai_thanh_toan = 'DA_THANH_TOAN'";
            result = jdbcTemplate.queryForMap(sql);
        }

        long doanhThu = ((Number) result.get("doanhThu")).longValue();
        int soDon = ((Number) result.get("soDon")).intValue();

        return ResponseEntity.ok(new ThongKeHomNayDTO(doanhThu, soDon));
    }

    /**
     * Thống kê theo ngày
     */
    @GetMapping("/theo-ngay")
    public ResponseEntity<List<ThongKeNgayDTO>> thongKeTheoNgay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {

        List<ThongKeNgayDTO> data;
        if (userId != null) {
            data = donHangRepository.thongKeTheoNgay(userId, from, to);
        } else {
            data = donHangRepository.thongKeTheoNgayOld(from, to);
        }
        return ResponseEntity.ok(data);
    }

    /**
     * Thống kê theo tháng
     */
    @GetMapping("/theo-thang")
    public ResponseEntity<List<ThongKeThangDTO>> thongKeTheoThang(
            @RequestParam int year,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {

        List<ThongKeThangDTO> data;
        if (userId != null) {
            data = donHangRepository.thongKeTheoThang(userId, year);
        } else {
            data = donHangRepository.thongKeTheoThangOld(year);
        }
        return ResponseEntity.ok(data);
    }

    /**
     * Thống kê theo quý
     */
    @GetMapping("/theo-quy")
    public ResponseEntity<List<ThongKeQuyDTO>> thongKeTheoQuy(
            @RequestParam int year,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {

        List<ThongKeQuyDTO> data;
        if (userId != null) {
            data = donHangRepository.thongKeTheoQuy(userId, year);
        } else {
            data = donHangRepository.thongKeTheoQuyOld(year);
        }
        return ResponseEntity.ok(data);
    }

    /**
     * Top món bán chạy
     */
    @GetMapping("/top-mon")
    public ResponseEntity<List<TopMonDTO>> topMon(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer year,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {

        List<TopMonDTO> data;
        if (from != null && to != null) {
            if (userId != null) {
                data = donHangRepository.topMonTheoKhoang(userId, from, to);
            } else {
                data = donHangRepository.topMonTheoKhoangOld(from, to);
            }
        } else {
            int y = (year != null) ? year : LocalDate.now().getYear();
            if (userId != null) {
                data = donHangRepository.topMonTheoNam(userId, y);
            } else {
                data = donHangRepository.topMonTheoNamOld(y);
            }
        }
        return ResponseEntity.ok(data);
    }

    /**
     * Lấy danh sách đơn hàng theo ngày
     */
    @GetMapping("/don-hang-theo-ngay")
    public ResponseEntity<List<Map<String, Object>>> donHangTheoNgay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        
        String sql;
        List<Map<String, Object>> result;
        
        if (userId != null) {
            sql = "SELECT dh.id, dh.id_ban as idBan, b.so_ban as soBan, dh.tong_tien as tongTien, dh.trang_thai_don as trangThaiDon, " +
                  "COUNT(ct.id) as soMon " +
                  "FROM don_hang dh " +
                  "LEFT JOIN Ban b ON dh.id_ban = b.id " +
                  "LEFT JOIN chi_tiet_don_hang ct ON dh.id = ct.id_don_hang " +
                  "WHERE CAST(dh.ngay_tao AS DATE) = ? AND dh.trang_thai_thanh_toan = 'DA_THANH_TOAN' AND dh.tai_khoan_id = ? " +
                  "GROUP BY dh.id, dh.id_ban, b.so_ban, dh.tong_tien, dh.trang_thai_don " +
                  "ORDER BY dh.id DESC";
            result = jdbcTemplate.queryForList(sql, ngay, userId);
        } else {
            sql = "SELECT dh.id, dh.id_ban as idBan, b.so_ban as soBan, dh.tong_tien as tongTien, dh.trang_thai_don as trangThaiDon, " +
                  "COUNT(ct.id) as soMon " +
                  "FROM don_hang dh " +
                  "LEFT JOIN Ban b ON dh.id_ban = b.id " +
                  "LEFT JOIN chi_tiet_don_hang ct ON dh.id = ct.id_don_hang " +
                  "WHERE CAST(dh.ngay_tao AS DATE) = ? AND dh.trang_thai_thanh_toan = 'DA_THANH_TOAN' " +
                  "GROUP BY dh.id, dh.id_ban, b.so_ban, dh.tong_tien, dh.trang_thai_don " +
                  "ORDER BY dh.id DESC";
            result = jdbcTemplate.queryForList(sql, ngay);
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * Lấy chi tiết một đơn hàng
     */
    @GetMapping("/chi-tiet-don-hang/{donHangId}")
    public ResponseEntity<List<Map<String, Object>>> chiTietDonHang(
            @PathVariable Integer donHangId,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        
        String sql = "SELECT ct.id, m.ten_mon as tenMon, ct.so_luong as soLuong, " +
                     "ct.don_gia as donGia, (ct.so_luong * ct.don_gia) as thanhTien " +
                     "FROM chi_tiet_don_hang ct " +
                     "JOIN mon_an m ON ct.id_mon_an = m.id " +
                     "WHERE ct.id_don_hang = ? " +
                     "ORDER BY ct.id";
        
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, donHangId);
        return ResponseEntity.ok(result);
    }

    /**
     * Lấy danh sách đơn hàng theo tháng
     */
    @GetMapping("/don-hang-theo-thang")
    public ResponseEntity<List<Map<String, Object>>> donHangTheoThang(
            @RequestParam int thang,
            @RequestParam int nam,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        
        String sql;
        List<Map<String, Object>> result;
        
        if (userId != null) {
            sql = "SELECT dh.id, dh.id_ban as idBan, b.so_ban as soBan, dh.tong_tien as tongTien, dh.trang_thai_don as trangThaiDon, " +
                  "COUNT(ct.id) as soMon, CAST(dh.ngay_tao AS DATE) as ngay " +
                  "FROM don_hang dh " +
                  "LEFT JOIN Ban b ON dh.id_ban = b.id " +
                  "LEFT JOIN chi_tiet_don_hang ct ON dh.id = ct.id_don_hang " +
                  "WHERE YEAR(dh.ngay_tao) = ? AND MONTH(dh.ngay_tao) = ? AND dh.trang_thai_thanh_toan = 'DA_THANH_TOAN' AND dh.tai_khoan_id = ? " +
                  "GROUP BY dh.id, dh.id_ban, b.so_ban, dh.tong_tien, dh.trang_thai_don, CAST(dh.ngay_tao AS DATE) " +
                  "ORDER BY dh.id DESC";
            result = jdbcTemplate.queryForList(sql, nam, thang, userId);
        } else {
            sql = "SELECT dh.id, dh.id_ban as idBan, b.so_ban as soBan, dh.tong_tien as tongTien, dh.trang_thai_don as trangThaiDon, " +
                  "COUNT(ct.id) as soMon, CAST(dh.ngay_tao AS DATE) as ngay " +
                  "FROM don_hang dh " +
                  "LEFT JOIN Ban b ON dh.id_ban = b.id " +
                  "LEFT JOIN chi_tiet_don_hang ct ON dh.id = ct.id_don_hang " +
                  "WHERE YEAR(dh.ngay_tao) = ? AND MONTH(dh.ngay_tao) = ? AND dh.trang_thai_thanh_toan = 'DA_THANH_TOAN' " +
                  "GROUP BY dh.id, dh.id_ban, b.so_ban, dh.tong_tien, dh.trang_thai_don, CAST(dh.ngay_tao AS DATE) " +
                  "ORDER BY dh.id DESC";
            result = jdbcTemplate.queryForList(sql, nam, thang);
        }
        
        return ResponseEntity.ok(result);
    }
}