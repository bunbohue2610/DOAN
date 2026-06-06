-- ====================================================================
-- SCRIPT CẬP NHẬT DATABASE HỖTRỢ MULTI-TENANCY (THEO TÀI KHOẢN)
-- Chạy script này để cập nhật cơ sở dữ liệu
-- ====================================================================

-- 1. Thêm cột tai_khoan_id vào bảng MonAn
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'MonAn' AND COLUMN_NAME = 'tai_khoan_id')
BEGIN
    ALTER TABLE MonAn ADD tai_khoan_id INT NULL;
    ALTER TABLE MonAn ADD CONSTRAINT FK_MonAn_TaiKhoan 
        FOREIGN KEY (tai_khoan_id) REFERENCES tai_khoan(id) ON DELETE CASCADE;
    CREATE NONCLUSTERED INDEX IX_MonAn_TaiKhoanId ON MonAn(tai_khoan_id);
END;
GO

-- 2. Thêm cột tai_khoan_id vào bảng Ban
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Ban' AND COLUMN_NAME = 'tai_khoan_id')
BEGIN
    ALTER TABLE Ban ADD tai_khoan_id INT NULL;
    ALTER TABLE Ban ADD CONSTRAINT FK_Ban_TaiKhoan 
        FOREIGN KEY (tai_khoan_id) REFERENCES tai_khoan(id) ON DELETE CASCADE;
    CREATE NONCLUSTERED INDEX IX_Ban_TaiKhoanId ON Ban(tai_khoan_id);
    -- Bỏ constraint unique cũ nếu có
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.CONSTRAINTS WHERE TABLE_NAME = 'Ban' AND CONSTRAINT_NAME = 'UQ_Ban_SoBan')
    BEGIN
        ALTER TABLE Ban DROP CONSTRAINT UQ_Ban_SoBan;
    END;
    -- Thêm constraint unique mới (so_ban, tai_khoan_id)
    ALTER TABLE Ban ADD CONSTRAINT UQ_Ban_SoBan_TaiKhoan UNIQUE (so_ban, tai_khoan_id);
END;
GO

-- 3. Thêm cột tai_khoan_id vào bảng danh_muc
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'danh_muc' AND COLUMN_NAME = 'tai_khoan_id')
BEGIN
    ALTER TABLE danh_muc ADD tai_khoan_id INT NULL;
    ALTER TABLE danh_muc ADD CONSTRAINT FK_DanhMuc_TaiKhoan 
        FOREIGN KEY (tai_khoan_id) REFERENCES tai_khoan(id) ON DELETE CASCADE;
    CREATE NONCLUSTERED INDEX IX_DanhMuc_TaiKhoanId ON danh_muc(tai_khoan_id);
END;
GO

-- 4. Thêm cột tai_khoan_id vào bảng DonHang
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'DonHang' AND COLUMN_NAME = 'tai_khoan_id')
BEGIN
    ALTER TABLE DonHang ADD tai_khoan_id INT NULL;
    ALTER TABLE DonHang ADD CONSTRAINT FK_DonHang_TaiKhoan 
        FOREIGN KEY (tai_khoan_id) REFERENCES tai_khoan(id) ON DELETE CASCADE;
    CREATE NONCLUSTERED INDEX IX_DonHang_TaiKhoanId ON DonHang(tai_khoan_id);
END;
GO

-- 5. Thêm cột ghi_chu vào bảng ChiTietDonHang
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'ChiTietDonHang' AND COLUMN_NAME = 'ghi_chu')
BEGIN
    ALTER TABLE ChiTietDonHang ADD ghi_chu NVARCHAR(500) NULL;
    PRINT 'Thêm cột ghi_chu vào ChiTietDonHang thành công!';
END;
GO

-- 6. Thêm các cột cho Voucher (Kiểu áp dụng + Danh sách sản phẩm/danh mục)
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Voucher' AND COLUMN_NAME = 'kieu_ap_dung')
BEGIN
    ALTER TABLE Voucher ADD kieu_ap_dung VARCHAR(50) DEFAULT 'TOAN_BAN';
    ALTER TABLE Voucher ADD danh_sach_san_pham NVARCHAR(MAX) NULL;
    ALTER TABLE Voucher ADD danh_sach_danh_muc NVARCHAR(MAX) NULL;
    PRINT 'Thêm các cột cho Voucher thành công!';
END;
GO

-- 7. Thêm cột tai_khoan_id vào bảng Voucher (để mỗi admin có voucher riêng)
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'Voucher' AND COLUMN_NAME = 'tai_khoan_id')
BEGIN
    ALTER TABLE Voucher ADD tai_khoan_id INT NULL;
    ALTER TABLE Voucher ADD CONSTRAINT FK_Voucher_TaiKhoan 
        FOREIGN KEY (tai_khoan_id) REFERENCES tai_khoan(id) ON DELETE CASCADE;
    CREATE NONCLUSTERED INDEX IX_Voucher_TaiKhoanId ON Voucher(tai_khoan_id);
    -- Bỏ constraint unique cũ nếu có trên ma_code
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.CONSTRAINTS WHERE TABLE_NAME = 'Voucher' AND CONSTRAINT_NAME = 'UQ_Voucher_MaCode')
    BEGIN
        ALTER TABLE Voucher DROP CONSTRAINT UQ_Voucher_MaCode;
    END;
    -- Thêm constraint unique mới (ma_code, tai_khoan_id)
    ALTER TABLE Voucher ADD CONSTRAINT UQ_Voucher_MaCode_TaiKhoan UNIQUE (ma_code, tai_khoan_id);
    PRINT 'Thêm tai_khoan_id vào Voucher thành công!';
END;
GO

PRINT 'Cập nhật schema hoàn tất!';
PRINT 'Lưu ý: Bạn cần cập nhật các dòng dữ liệu hiện tại với tai_khoan_id trước khi sử dụng ứng dụng';
GO
