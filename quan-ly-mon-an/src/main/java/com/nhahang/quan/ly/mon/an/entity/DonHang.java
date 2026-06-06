package com.nhahang.quan.ly.mon.an.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "DonHang")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonHang {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tai_khoan_id", nullable = false)
    private TaiKhoan taiKhoan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ban", nullable = false)
    private Ban ban;

    @Column(name = "tong_tien", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal tongTien = BigDecimal.ZERO;

    @Column(name = "trang_thai_don", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TrangThaiDon trangThaiDon = TrangThaiDon.CHO_DUYET;

    @Column(name = "trang_thai_thanh_toan", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TrangThaiThanhToan trangThaiThanhToan = TrangThaiThanhToan.CHUA_THANH_TOAN;

    @Column(name = "ngay_tao", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayTao = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (ngayTao == null) ngayTao = LocalDateTime.now();
    }

    public enum TrangThaiDon { CHO_DUYET, DANG_LAM, DA_PHUC_VU, DA_HUY }
    public enum TrangThaiThanhToan { DA_THANH_TOAN, CHUA_THANH_TOAN }
}