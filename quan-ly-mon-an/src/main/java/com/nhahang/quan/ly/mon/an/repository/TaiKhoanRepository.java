package com.nhahang.quan.ly.mon.an.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nhahang.quan.ly.mon.an.entity.TaiKhoan;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {
    // Hàm này giúp Spring Boot tự động mò trong SQL xem có ông nào tên đăng nhập khớp không
    Optional<TaiKhoan> findByUsername(String username);
    
    // Tìm tài khoản qua Google ID
    Optional<TaiKhoan> findByGoogleId(String googleId);
    
    // Tìm tài khoản qua email
    Optional<TaiKhoan> findByEmail(String email);
}