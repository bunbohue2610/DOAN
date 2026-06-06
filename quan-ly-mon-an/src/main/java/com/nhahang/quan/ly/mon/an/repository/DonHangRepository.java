package com.nhahang.quan.ly.mon.an.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhahang.quan.ly.mon.an.dto.ThongKeNgayDTO;
import com.nhahang.quan.ly.mon.an.dto.ThongKeQuyDTO;
import com.nhahang.quan.ly.mon.an.dto.ThongKeThangDTO;
import com.nhahang.quan.ly.mon.an.dto.TopMonDTO;
import com.nhahang.quan.ly.mon.an.entity.DonHang;

@Repository
public interface DonHangRepository extends JpaRepository<DonHang, Long> {

    // Lấy tất cả đơn hàng của một tài khoản
    List<DonHang> findByTaiKhoanId(Integer taiKhoanId);

    // ─── THEO NGÀY ────────────────────────────────────────────────────────────

    @Query(value = """
        SELECT
            CAST(d.ngay_tao AS DATE)             AS ngay,
            COALESCE(SUM(d.tong_tien), 0)        AS doanhThu,
            COUNT(d.id)                           AS soDon
        FROM don_hang d
        WHERE d.tai_khoan_id = :taiKhoanId
          AND d.trang_thai_thanh_toan = 'DA_THANH_TOAN'
          AND CAST(d.ngay_tao AS DATE) BETWEEN :from AND :to
        GROUP BY CAST(d.ngay_tao AS DATE)
        ORDER BY ngay ASC
        """, nativeQuery = true)
    List<ThongKeNgayDTO> thongKeTheoNgay(
            @Param("taiKhoanId") Integer taiKhoanId,
            @Param("from") LocalDate from,
            @Param("to")   LocalDate to);

    @Query(value = """
        SELECT
            CAST(d.ngay_tao AS DATE)             AS ngay,
            COALESCE(SUM(d.tong_tien), 0)        AS doanhThu,
            COUNT(d.id)                           AS soDon
        FROM don_hang d
        WHERE d.trang_thai_thanh_toan = 'PAID'
          AND CAST(d.ngay_tao AS DATE) BETWEEN :from AND :to
        GROUP BY CAST(d.ngay_tao AS DATE)
        ORDER BY ngay ASC
        """, nativeQuery = true)
    List<ThongKeNgayDTO> thongKeTheoNgayOld(
            @Param("from") LocalDate from,
            @Param("to")   LocalDate to);

    // ─── THEO THÁNG ───────────────────────────────────────────────────────────

    @Query(value = """
        SELECT
            MONTH(d.ngay_tao)                    AS thang,
            COALESCE(SUM(d.tong_tien), 0)        AS doanhThu,
            COUNT(d.id)                           AS soDon
        FROM don_hang d
        WHERE d.tai_khoan_id = :taiKhoanId
          AND d.trang_thai_thanh_toan = 'DA_THANH_TOAN'
          AND YEAR(d.ngay_tao) = :year
        GROUP BY MONTH(d.ngay_tao)
        ORDER BY thang ASC
        """, nativeQuery = true)
    List<ThongKeThangDTO> thongKeTheoThang(
            @Param("taiKhoanId") Integer taiKhoanId,
            @Param("year") int year);

    @Query(value = """
        SELECT
            MONTH(d.ngay_tao)                    AS thang,
            COALESCE(SUM(d.tong_tien), 0)        AS doanhThu,
            COUNT(d.id)                           AS soDon
        FROM don_hang d
        WHERE d.trang_thai_thanh_toan = 'PAID'
          AND YEAR(d.ngay_tao) = :year
        GROUP BY MONTH(d.ngay_tao)
        ORDER BY thang ASC
        """, nativeQuery = true)
    List<ThongKeThangDTO> thongKeTheoThangOld(@Param("year") int year);

    // ─── THEO QUÝ ─────────────────────────────────────────────────────────────

    @Query(value = """
        SELECT
            DATEPART(QUARTER, d.ngay_tao)        AS quy,
            COALESCE(SUM(d.tong_tien), 0)        AS doanhThu,
            COUNT(d.id)                           AS soDon
        FROM don_hang d
        WHERE d.tai_khoan_id = :taiKhoanId
          AND d.trang_thai_thanh_toan = 'DA_THANH_TOAN'
          AND YEAR(d.ngay_tao) = :year
        GROUP BY DATEPART(QUARTER, d.ngay_tao)
        ORDER BY quy ASC
        """, nativeQuery = true)
    List<ThongKeQuyDTO> thongKeTheoQuy(
            @Param("taiKhoanId") Integer taiKhoanId,
            @Param("year") int year);

    @Query(value = """
        SELECT
            DATEPART(QUARTER, d.ngay_tao)        AS quy,
            COALESCE(SUM(d.tong_tien), 0)        AS doanhThu,
            COUNT(d.id)                           AS soDon
        FROM don_hang d
        WHERE d.trang_thai_thanh_toan = 'PAID'
          AND YEAR(d.ngay_tao) = :year
        GROUP BY DATEPART(QUARTER, d.ngay_tao)
        ORDER BY quy ASC
        """, nativeQuery = true)
    List<ThongKeQuyDTO> thongKeTheoQuyOld(@Param("year") int year);

    // ─── TOP MÓN ──────────────────────────────────────────────────────────────

    @Query(value = """
        SELECT TOP 10
            m.ten_mon                              AS tenMon,
            SUM(ct.so_luong)                       AS soLuong,
            SUM(ct.so_luong * ct.don_gia)          AS doanhThu
        FROM chi_tiet_don_hang ct
        JOIN mon_an m    ON ct.id_mon_an  = m.id
        JOIN don_hang d  ON ct.id_don_hang = d.id
        WHERE d.tai_khoan_id = :taiKhoanId
          AND d.trang_thai_thanh_toan = 'DA_THANH_TOAN'
          AND CAST(d.ngay_tao AS DATE) BETWEEN :from AND :to
        GROUP BY m.id, m.ten_mon
        ORDER BY soLuong DESC
        """, nativeQuery = true)
    List<TopMonDTO> topMonTheoKhoang(
            @Param("taiKhoanId") Integer taiKhoanId,
            @Param("from") LocalDate from,
            @Param("to")   LocalDate to);

    @Query(value = """
        SELECT TOP 10
            m.ten_mon                              AS tenMon,
            SUM(ct.so_luong)                       AS soLuong,
            SUM(ct.so_luong * ct.don_gia)          AS doanhThu
        FROM chi_tiet_don_hang ct
        JOIN mon_an m    ON ct.id_mon_an  = m.id
        JOIN don_hang d  ON ct.id_don_hang = d.id
        WHERE d.tai_khoan_id = :taiKhoanId
          AND d.trang_thai_thanh_toan = 'DA_THANH_TOAN'
          AND YEAR(d.ngay_tao) = :year
        GROUP BY m.id, m.ten_mon
        ORDER BY soLuong DESC
        """, nativeQuery = true)
    List<TopMonDTO> topMonTheoNam(
            @Param("taiKhoanId") Integer taiKhoanId,
            @Param("year") int year);

    @Query(value = """
        SELECT TOP 10
            m.ten_mon                              AS tenMon,
            SUM(ct.so_luong)                       AS soLuong,
            SUM(ct.so_luong * ct.don_gia)          AS doanhThu
        FROM chi_tiet_don_hang ct
        JOIN mon_an m    ON ct.id_mon_an  = m.id
        JOIN don_hang d  ON ct.id_don_hang = d.id
        WHERE d.trang_thai_thanh_toan = 'PAID'
          AND CAST(d.ngay_tao AS DATE) BETWEEN :from AND :to
        GROUP BY m.id, m.ten_mon
        ORDER BY soLuong DESC
        """, nativeQuery = true)
    List<TopMonDTO> topMonTheoKhoangOld(
            @Param("from") LocalDate from,
            @Param("to")   LocalDate to);

    @Query(value = """
        SELECT TOP 10
            m.ten_mon                              AS tenMon,
            SUM(ct.so_luong)                       AS soLuong,
            SUM(ct.so_luong * ct.don_gia)          AS doanhThu
        FROM chi_tiet_don_hang ct
        JOIN mon_an m    ON ct.id_mon_an  = m.id
        JOIN don_hang d  ON ct.id_don_hang = d.id
        WHERE d.trang_thai_thanh_toan = 'PAID'
          AND YEAR(d.ngay_tao) = :year
        GROUP BY m.id, m.ten_mon
        ORDER BY soLuong DESC
        """, nativeQuery = true)
    List<TopMonDTO> topMonTheoNamOld(@Param("year") int year);
}