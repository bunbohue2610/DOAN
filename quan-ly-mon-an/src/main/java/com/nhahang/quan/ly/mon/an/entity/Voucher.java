package com.nhahang.quan.ly.mon.an.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Entity
@Table(name = "Voucher")
@Data
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "ma_code", nullable = false, length = 50)
    private String maCode;

    @Column(name = "phan_tram_giam", nullable = false)
    private Integer phanTramGiam;

    @Column(name = "so_luong")
    @Builder.Default
    private Integer soLuong = 100;

    @Column(name = "ngay_het_han")
    @Temporal(TemporalType.TIMESTAMP)
    private Date ngayHetHan;

    @Column(name = "trang_thai")
    @Builder.Default
    private Boolean trangThai = true;

    @Column(name = "kieu_ap_dung", length = 50, nullable = false)
    @Builder.Default
    private String kieuApDung = "TOAN_BAN";

    @Column(name = "danh_sach_san_pham", columnDefinition = "NVARCHAR(MAX)")
    private String danhSachSanPham;

    @Column(name = "danh_sach_danh_muc", columnDefinition = "NVARCHAR(MAX)")
    private String danhSachDanhMuc;

    // ✅ Liên kết với TaiKhoan (Admin)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tai_khoan_id", nullable = true)
    private TaiKhoan taiKhoan;

    // ✅ Single no-args constructor — replaces @NoArgsConstructor
    // Needed by JPA; sets safe defaults that @Builder.Default can't cover here
    public Voucher() {
        this.kieuApDung = "TOAN_BAN";
        this.trangThai = true;
        this.soLuong = 100;
    }
}