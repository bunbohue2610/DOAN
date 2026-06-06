package com.nhahang.quan.ly.mon.an.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhahang.quan.ly.mon.an.entity.MonAn;

@Repository
public interface MonAnRepository extends JpaRepository<MonAn, Integer> {
    // Lấy tất cả món ăn của một tài khoản
    List<MonAn> findByTaiKhoanId(Integer taiKhoanId);
    
    // Lấy món ăn theo danh mục của một tài khoản
    List<MonAn> findByDanhMucIdAndTaiKhoanId(Integer danhMucId, Integer taiKhoanId);
    
    // Tìm kiếm món ăn theo tên của một tài khoản
    @Query("SELECT m FROM MonAn m WHERE m.taiKhoan.id = :taiKhoanId AND LOWER(m.tenMon) LIKE LOWER(CONCAT('%', :kw, '%'))")
    List<MonAn> timKiemTheoTen(@Param("taiKhoanId") Integer taiKhoanId, @Param("kw") String keyword);
    
    // Tìm kiếm món ăn theo tên (phương thức cũ - không filter user)
    @Query("SELECT m FROM MonAn m JOIN FETCH m.danhMuc WHERE LOWER(m.tenMon) LIKE LOWER(CONCAT('%', :kw, '%'))")
    List<MonAn> timKiemTheoTenOld(@Param("kw") String keyword);
    
    // Tìm kiếm theo danh mục ID (cũ)
    List<MonAn> findByDanhMucId(Integer danhMucId);
}