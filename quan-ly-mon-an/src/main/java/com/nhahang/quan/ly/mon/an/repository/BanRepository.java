package com.nhahang.quan.ly.mon.an.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nhahang.quan.ly.mon.an.entity.Ban;

@Repository
public interface BanRepository extends JpaRepository<Ban, Integer> {
    // Lấy tất cả bàn của một tài khoản
    List<Ban> findByTaiKhoanId(Integer taiKhoanId);
    
    // Lấy bàn theo số bàn và tài khoản - với TRIM
    @Query("SELECT b FROM Ban b WHERE TRIM(b.soBan) = TRIM(:soBan) AND b.taiKhoan.id = :taiKhoanId")
    Ban findByTaiKhoanIdAndSoBan(@Param("taiKhoanId") Integer taiKhoanId, @Param("soBan") String soBan);
    
    // Lấy bàn theo trạng thái của một tài khoản
    List<Ban> findByTaiKhoanIdAndTrangThai(Integer taiKhoanId, Ban.TrangThaiBan trangThai);
    
    // Kiểm tra sự tồn tại bàn theo số bàn
    @Query("SELECT COUNT(b) > 0 FROM Ban b WHERE TRIM(b.soBan) = TRIM(:soBan)")
    boolean existsBySoBan(@Param("soBan") String soBan);
    
    // Tìm bàn theo số bàn - với TRIM (cũ - không filter user)
    @Query("SELECT b FROM Ban b WHERE TRIM(b.soBan) = TRIM(:soBan)")
    Ban findBySoBan(@Param("soBan") String soBan);
    
    // Lấy bàn theo trạng thái (cũ - không filter user)
    List<Ban> findByTrangThai(Ban.TrangThaiBan trangThai);

    boolean existsByTaiKhoanIdAndSoBan(Integer taiKhoanId, String soBan);
    
}
