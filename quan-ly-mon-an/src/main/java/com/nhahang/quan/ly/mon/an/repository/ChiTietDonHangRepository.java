package com.nhahang.quan.ly.mon.an.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.nhahang.quan.ly.mon.an.entity.ChiTietDonHang;

@Repository
public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, Integer> {
    List<ChiTietDonHang> findByDonHangId(Integer donHangId);
    
    // Xóa tất cả chi tiết đơn hàng liên kết với một món ăn
    @Modifying
    @Transactional
    @Query("DELETE FROM ChiTietDonHang ct WHERE ct.monAn.id = :monAnId")
    void deleteByMonAnId(@Param("monAnId") Integer monAnId);
}