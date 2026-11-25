package com.mycompany.lazar_app.ui.dialog;

import com.github.sarxos.webcam.Webcam;
import com.mycompany.lazar_app.LanguageManager;
import com.mycompany.lazar_app.ui.WindowManager;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Consumer;

/** Owns the camera for the dialog lifetime; all device and disk IO runs off-thread. */
public final class CameraCaptureDialog {
    private CameraCaptureDialog() {}

    public static void show(Window owner, Consumer<Path> onCapture) {
        var texts = LanguageManager.getBundle();
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(texts.getString("addMember.camera"));
        ImageView preview = new ImageView();
        preview.setFitWidth(480); preview.setFitHeight(340); preview.setPreserveRatio(true);
        Label message = new Label(texts.getString("addMember.opening"));
        message.setWrapText(true);
        ButtonType captureType=new ButtonType(texts.getString("addMember.capture"),ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().setAll(captureType,ButtonType.CANCEL);
        Button capture = (Button)dialog.getDialogPane().lookupButton(captureType);
        capture.setDisable(true);
        VBox content = new VBox(14, preview, message);
        content.setNodeOrientation(LanguageManager.isArabic()
                ? javafx.geometry.NodeOrientation.RIGHT_TO_LEFT : javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
        dialog.getDialogPane().setContent(content);
        preview.fitWidthProperty().bind(javafx.beans.binding.Bindings.createDoubleBinding(
                ()->Math.min(480,Math.max(1,dialog.getDialogPane().getWidth()-64)),dialog.getDialogPane().widthProperty()));
        ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "member-camera"); t.setDaemon(true); return t;
        });
        AtomicBoolean closed = new AtomicBoolean();
        AtomicBoolean requested = new AtomicBoolean();
        AtomicBoolean pending = new AtomicBoolean();
        capture.addEventFilter(javafx.event.ActionEvent.ACTION,e -> { e.consume(); capture.setDisable(true); requested.set(true); });
        dialog.setOnHidden(e -> { closed.set(true); worker.shutdown(); });
        worker.submit(() -> {
            Webcam camera = null;
            try {
                camera = Webcam.getDefault();
                if (camera == null || !camera.open()) throw new IOException("Camera unavailable");
                while (!closed.get()) {
                    BufferedImage frame = camera.getImage();
                    if (frame == null) { Thread.sleep(80); continue; }
                    if (requested.getAndSet(false)) {
                        Path directory = com.mycompany.lazar_app.util.AppPaths.dataDirectory().resolve("member-photos");
                        Files.createDirectories(directory);
                        Path file = Files.createTempFile(directory, "member-", ".png");
                        try {
                            if (!ImageIO.write(frame, "PNG", file.toFile())) throw new IOException("PNG unavailable");
                        } catch (Exception error) { Files.deleteIfExists(file); throw error; }
                        Platform.runLater(() -> {
                            if (!closed.get()) { onCapture.accept(file); dialog.close(); }
                        });
                        break;
                    }
                    if (pending.compareAndSet(false, true)) {
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        ImageIO.write(frame, "PNG", bytes);
                        Image image = new Image(new ByteArrayInputStream(bytes.toByteArray()));
                        Platform.runLater(() -> {
                            pending.set(false);
                            if (!closed.get()) {
                                preview.setImage(image); message.setText(""); capture.setDisable(false);
                            }
                        });
                    }
                    Thread.sleep(100);
                }
            } catch (Exception | LinkageError error) {
                Platform.runLater(() -> {
                    if (!closed.get()) { capture.setDisable(true); message.setText(texts.getString("addMember.error")); }
                });
            } finally {
                if (camera != null && camera.isOpen()) camera.close();
                worker.shutdown();
            }
        });
        DialogStyler.style(dialog);
        WindowManager.showResponsiveDialogAndWait(dialog, owner, 580, 560);
    }
}
