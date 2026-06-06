package com.nhahang.quan.ly.mon.an.dto;

public class UpdateProfileDTO {
    private Integer id;
    private String hoTen;
    private String email;
    private String passwordCu;
    private String passwordMoi;
    private String passwordMoiLan2;

    // Constructor
    public UpdateProfileDTO() {}

    // Getter & Setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordCu() { return passwordCu; }
    public void setPasswordCu(String passwordCu) { this.passwordCu = passwordCu; }

    public String getPasswordMoi() { return passwordMoi; }
    public void setPasswordMoi(String passwordMoi) { this.passwordMoi = passwordMoi; }

    public String getPasswordMoiLan2() { return passwordMoiLan2; }
    public void setPasswordMoiLan2(String passwordMoiLan2) { this.passwordMoiLan2 = passwordMoiLan2; }
}
