package com.nhahang.quan.ly.mon.an.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nhahang.quan.ly.mon.an.entity.Voucher;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    // Tìm mã voucher đang còn kích hoạt (dùng cho khách hàng)
    Optional<Voucher> findByMaCodeAndTrangThai(String maCode, Boolean trangThai);
    
    // Tìm tất cả voucher của một admin
    List<Voucher> findByTaiKhoanId(Integer taiKhoanId);
    
    // Tìm voucher theo mã code và admin ID
    Optional<Voucher> findByMaCodeAndTaiKhoanId(String maCode, Integer taiKhoanId);
}