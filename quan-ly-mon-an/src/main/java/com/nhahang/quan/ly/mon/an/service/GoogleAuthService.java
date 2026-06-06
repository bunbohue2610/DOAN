package com.nhahang.quan.ly.mon.an.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.nhahang.quan.ly.mon.an.entity.TaiKhoan;
import com.nhahang.quan.ly.mon.an.repository.TaiKhoanRepository;

@Service
public class GoogleAuthService {

    @Autowired
    private TaiKhoanRepository taiKhoanRepository;

    @Value("${google.oauth2.clientId}")
    private String googleClientId;

    /**
     * Xác minh ID Token từ Google và trả về thông tin user
     */
    public TaiKhoan verifyGoogleToken(String idTokenString) throws Exception {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), new GsonFactory())
                .setAudience(java.util.Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        
        if (idToken == null) {
            throw new Exception("Invalid Google ID Token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        // Lấy thông tin từ Google
        String googleId = payload.getSubject();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        Optional<TaiKhoan> existingUser = taiKhoanRepository.findByGoogleId(googleId);

        if (existingUser.isPresent()) {
           
            TaiKhoan user = existingUser.get();
            user.setEmail(email);
            user.setHoTen(name);
            user.setAuthProvider("GOOGLE");
            return taiKhoanRepository.save(user);
        } else {
           
            TaiKhoan newUser = new TaiKhoan();
            newUser.setGoogleId(googleId);
            newUser.setEmail(email);
            newUser.setHoTen(name);
            newUser.setUsername(email); 
            newUser.setPassword(""); 
            newUser.setVaiTro("ADMIN"); 
            newUser.setAuthProvider("GOOGLE");
            
            return taiKhoanRepository.save(newUser);
        }
    }
}
