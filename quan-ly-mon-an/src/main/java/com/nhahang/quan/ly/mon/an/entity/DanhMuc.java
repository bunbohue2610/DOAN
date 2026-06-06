package com.nhahang.quan.ly.mon.an.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "danh_muc")
public class DanhMuc {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tai_khoan_id", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "ten_danh_muc")
    private String tenDanhMuc;

    // Thêm trường phần trăm giảm giá áp dụng cho toàn bộ danh mục (mặc định là 0%)
    @Column(name = "phan_tram_giam", columnDefinition = "int default 0")
    private Integer phanTramGiam = 0;

    // --- TỰ TẠO GETTER/SETTER THỦ CÔNG ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public TaiKhoan getTaiKhoan() { return taiKhoan; }
    public void setTaiKhoan(TaiKhoan taiKhoan) { this.taiKhoan = taiKhoan; }

    public String getTenDanhMuc() { return tenDanhMuc; }
    public void setTenDanhMuc(String tenDanhMuc) { this.tenDanhMuc = tenDanhMuc; }

    public Integer getPhanTramGiam() { return phanTramGiam; }
    public void setPhanTramGiam(Integer phanTramGiam) { this.phanTramGiam = phanTramGiam; }
}