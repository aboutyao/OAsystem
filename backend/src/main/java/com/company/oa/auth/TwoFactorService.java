package com.company.oa.auth;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class TwoFactorService {
    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final DefaultCodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator);

    public String generateSecret() {
        return secretGenerator.generate();
    }

    public String getQrCodeUri(String secret, String username, String issuer) {
        QrData data = new QrData.Builder()
                .label(issuer + ":" + username)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        return data.getUri();
    }

    public String getQrCodeImage(String secret, String username, String issuer) {
        try {
            String uri = getQrCodeUri(secret, username, issuer);
            ZxingPngQrGenerator generator = new ZxingPngQrGenerator();
            byte[] svg = generator.generate(uri);
            return Base64.getEncoder().encodeToString(svg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }
}
