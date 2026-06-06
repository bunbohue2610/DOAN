package com.nhahang.quan.ly.mon.an.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "MonAn")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonAn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tai_khoan_id", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "ten_mon", nullable = false, length = 150)
    private String tenMon;

    @Column(name = "gia_tien", nullable = false, precision = 10, scale = 2)
    private BigDecimal giaTien;

    @Column(name = "mo_ta", length = 500)
    private String moTa;

    @Column(name = "hinh_anh", length = 500)
    private String hinhAnh;

    @ManyToOne
    @JoinColumn(name = "danh_muc_id")
    private DanhMuc danhMuc;

    // Thêm trường phần trăm giảm giá (mặc định là 0%)
    @Column(name = "phan_tram_giam")
    @Builder.Default
    private Integer phanTramGiam = 0;
}