package com.nhahang.quan.ly.mon.an.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nhahang.quan.ly.mon.an.entity.DanhMuc;

@Repository
public interface DanhMucRepository extends JpaRepository<DanhMuc, Integer> {
    // Lấy tất cả danh mục của một tài khoản
    List<DanhMuc> findByTaiKhoanId(Integer taiKhoanId);
    
    // Kiểm tra danh mục tồn tại theo tên và tài khoản
    boolean existsByTaiKhoanIdAndTenDanhMuc(Integer taiKhoanId, String tenDanhMuc);
    
    // Cũ - không filter user
    boolean existsByTenDanhMuc(String tenDanhMuc);
}