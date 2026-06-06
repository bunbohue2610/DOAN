package com.nhahang.quan.ly.mon.an.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nhahang.quan.ly.mon.an.dto.ApiResponse;
import com.nhahang.quan.ly.mon.an.dto.UpdateProfileDTO;
import com.nhahang.quan.ly.mon.an.entity.TaiKhoan;
import com.nhahang.quan.ly.mon.an.repository.TaiKhoanRepository;
import com.nhahang.quan.ly.mon.an.service.GoogleAuthService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Autowired
    private GoogleAuthService googleAuthService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");

        // 1. Tìm tài khoản trong Database
        Optional<TaiKhoan> userOpt = taiKhoanRepository.findByUsername(username);

        // 2. Nếu tài khoản tồn tại
        if (userOpt.isPresent()) {
            TaiKhoan user = userOpt.get();
            // 3. So sánh mật khẩu (Chưa mã hóa)
            if (user.getPassword().equals(password)) {
                // Trả về thông tin user kèm ID để frontend có thể gửi lên server
                Map<String, Object> response = new HashMap<>();
                response.put("id", user.getId());
                response.put("username", user.getUsername());
                response.put("hoTen", user.getHoTen());
                response.put("vaiTro", user.getVaiTro());
                
                return ResponseEntity.ok(new ApiResponse<>(true, "Đăng nhập thành công", response));
            }
        }
        
        // Trả về lỗi nếu sai tên hoặc sai pass
        return ResponseEntity.status(401).body(new ApiResponse<>(false, "Sai tài khoản hoặc mật khẩu", null));
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        String hoTen = payload.get("hoTen");

        // 1. Kiểm tra xem tên đăng nhập này đã có ai xài chưa
        if (taiKhoanRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Tên đăng nhập này đã tồn tại!", null));
        }

        // 2. Nếu chưa ai xài thì tạo mới
        TaiKhoan newAcc = new TaiKhoan();
        newAcc.setUsername(username);
        newAcc.setPassword(password);
        newAcc.setHoTen(hoTen);
        newAcc.setVaiTro("ADMIN"); // Mặc định cấp quyền Admin luôn

        // 3. Lưu thẳng xuống SQL Server
        taiKhoanRepository.save(newAcc);

        return ResponseEntity.ok(new ApiResponse<>(true, "Đăng ký thành công", null));
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> payload) {
        try {
            String idToken = payload.get("idToken");
            
            if (idToken == null || idToken.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "ID Token không được để trống", null));
            }

            // Xác minh token từ Google
            TaiKhoan user = googleAuthService.verifyGoogleToken(idToken);

            // Trả về thông tin user
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("hoTen", user.getHoTen());
            response.put("email", user.getEmail());
            response.put("vaiTro", user.getVaiTro());
            response.put("authProvider", user.getAuthProvider());

            return ResponseEntity.ok(new ApiResponse<>(true, "Đăng nhập Google thành công", response));

        } catch (Exception e) {
            return ResponseEntity.status(401).body(
                new ApiResponse<>(false, "Lỗi xác minh Google: " + e.getMessage(), null));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // API CHỈNH SỬA THÔNG TIN ADMIN
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/profile/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Integer id) {
        try {
            Optional<TaiKhoan> userOpt = taiKhoanRepository.findById(id);
            
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(404).body(
                    new ApiResponse<>(false, "Tài khoản không tồn tại", null));
            }

            TaiKhoan user = userOpt.get();
            
            // Trả về thông tin profile
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("hoTen", user.getHoTen());
            response.put("email", user.getEmail());
            response.put("vaiTro", user.getVaiTro());
            response.put("authProvider", user.getAuthProvider());

            return ResponseEntity.ok(new ApiResponse<>(true, "Lấy thông tin thành công", response));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                new ApiResponse<>(false, "Lỗi server: " + e.getMessage(), null));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileDTO updateDTO) {
        try {
            // 1. Lấy tài khoản từ ID
            Optional<TaiKhoan> userOpt = taiKhoanRepository.findById(updateDTO.getId());
            
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(404).body(
                    new ApiResponse<>(false, "Tài khoản không tồn tại", null));
            }

            TaiKhoan user = userOpt.get();

            // 2. Cập nhật họ tên
            if (updateDTO.getHoTen() != null && !updateDTO.getHoTen().trim().isEmpty()) {
                user.setHoTen(updateDTO.getHoTen().trim());
            }

            // 3. Cập nhật email
            if (updateDTO.getEmail() != null && !updateDTO.getEmail().trim().isEmpty()) {
                user.setEmail(updateDTO.getEmail().trim());
            }

            // 4. Cập nhật mật khẩu (nếu có)
            if (updateDTO.getPasswordMoi() != null && !updateDTO.getPasswordMoi().isEmpty()) {
                if ("GOOGLE".equalsIgnoreCase(user.getAuthProvider())) {
                    return ResponseEntity.status(400).body(
                        new ApiResponse<>(false, "Tài khoản Google không thể đổi mật khẩu trong hệ thống", null));
                }

                // Kiểm tra mật khẩu cũ
                if (!user.getPassword().equals(updateDTO.getPasswordCu())) {
                    return ResponseEntity.status(400).body(
                        new ApiResponse<>(false, "Mật khẩu cũ không đúng", null));
                }

                // Kiểm tra 2 mật khẩu mới có khớp không
                if (!updateDTO.getPasswordMoi().equals(updateDTO.getPasswordMoiLan2())) {
                    return ResponseEntity.status(400).body(
                        new ApiResponse<>(false, "Mật khẩu mới không khớp", null));
                }

                // Kiểm tra mật khẩu mới không được trùng với cũ
                if (updateDTO.getPasswordMoi().equals(updateDTO.getPasswordCu())) {
                    return ResponseEntity.status(400).body(
                        new ApiResponse<>(false, "Mật khẩu mới không được trùng với mật khẩu cũ", null));
                }

                // Cập nhật mật khẩu mới
                user.setPassword(updateDTO.getPasswordMoi());
            }

            // 5. Lưu lại vào Database
            taiKhoanRepository.save(user);

            // 6. Trả về thông tin đã cập nhật
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("hoTen", user.getHoTen());
            response.put("email", user.getEmail());
            response.put("vaiTro", user.getVaiTro());
            response.put("authProvider", user.getAuthProvider());

            return ResponseEntity.ok(new ApiResponse<>(true, "Cập nhật thông tin thành công", response));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                new ApiResponse<>(false, "Lỗi server: " + e.getMessage(), null));
        }
    }
}
