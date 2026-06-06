package com.nhahang.quan.ly.mon.an.entity;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "Ban",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UK_so_ban_tai_khoan",
            columnNames = {"so_ban", "tai_khoan_id"}
        )
        }
)
@Data  
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ban {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tai_khoan_id", nullable = false)
    private TaiKhoan taiKhoan;
    
    @Column(name = "so_ban", nullable = false, length = 10)
    private String soBan;
    
    @Column(name = "trang_thai", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TrangThaiBan trangThai = TrangThaiBan.TRONG;
    
    public enum TrangThaiBan { TRONG, CO_KHACH, DANG_CHO }
    
    // Explicit getters/setters for IDE recognition
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public String getSoBan() {
        return soBan;
    }
    
    public void setSoBan(String soBan) {
        this.soBan = soBan;
    }
    
    public TrangThaiBan getTrangThai() {
        return trangThai;
    }
    
    public void setTrangThai(TrangThaiBan trangThai) {
        this.trangThai = trangThai;
    }
}