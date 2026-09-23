package com.mycompany.lazar_app.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javafx.scene.image.Image;

public final class QrCodeGenerator {

    private QrCodeGenerator() {
    }

    public static Image generate(
            String content,
            int width,
            int height
    ) throws WriterException, IOException {

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "QR content cannot be empty."
            );
        }

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        BitMatrix bitMatrix = qrCodeWriter.encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height
        );

        ByteArrayOutputStream outputStream =
                new ByteArrayOutputStream();

        MatrixToImageWriter.writeToStream(
                bitMatrix,
                "PNG",
                outputStream
        );

        return new Image(
                new ByteArrayInputStream(
                        outputStream.toByteArray()
                )
        );
    }
}