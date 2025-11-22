package com.rebay.rebay_backend.S3.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.rebay.rebay_backend.user.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 s3Client;

    @Value("${AWS_BUCKET_NAME}")
    private String bucketName;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    // 이미지 크기 (1080x1080 픽셀)
    private static final int IMAGE_SIZE = 1080;


    public String uploadFile(MultipartFile file, String folder) {
        // S3 클라이언트가 설정되지 않았을 경우 예외 발생
        if (s3Client == null) {
            log.warn("S3 client not configured. Using local storage fallback.");
            throw new RuntimeException("S3 not configured");
        }

        // 파일 유효성 검사
        validateFile(file);

        // 고유한 파일명 생성
        String fileName = generateFileName(file, folder);

        try {
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new BadRequestException("Invalid image file");
            }

            // 이미지를 정사각형으로 크롭
            BufferedImage squareImage = cropToSquare(originalImage);

            // 이미지를 지정된 크기로 리사이징
            BufferedImage resizedImage = resizeImage(squareImage, IMAGE_SIZE, IMAGE_SIZE);

            // 리사이징된 이미지를 바이트 배열로 변환
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", outputStream);

            // S3 업로드를 위한 PutObjectRequest 생성
            PutObjectRequest putObjectRequest = getPutObjectRequest(file, outputStream, fileName);

            // S3에 파일 업로드
            s3Client.putObject(putObjectRequest);

            log.info("Successfully uploaded file to S3: {}", fileName);

            return fileName;
        } catch (IOException e) {
            log.error("Failed to upload file to S3", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    /**
     * S3에 파일을 업로드하기 위한 PutObjectRequest를 생성
     */
    private PutObjectRequest getPutObjectRequest(MultipartFile file, ByteArrayOutputStream outputStream, String fileName) {
        byte[] imageBytes = outputStream.toByteArray();
        InputStream inputStream = new ByteArrayInputStream(imageBytes);

        // S3에 저장될 파일의 메타데이터 설정
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("image/jpeg");
        metadata.setContentLength(imageBytes.length);

        return new PutObjectRequest(
                bucketName,
                fileName,
                inputStream,
                metadata
        );
    }


    public String generatePresignedUrl(String fileKey, int expirationMinutes) {
        if (s3Client == null || fileKey == null) {
            return null;
        }

        // URL 만료 시간 설정
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000L * 60 * expirationMinutes;
        expiration.setTime(expTimeMillis);

        // Pre-signed URL 요청 생성
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
                bucketName,
                fileKey
        )
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);

        // Pre-signed URL 생성 및 반환
        URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }

    /**
     * 이미지 정사각
     */
    private BufferedImage cropToSquare(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        int squareSize = Math.min(width, height);

        int x = (width - squareSize) / 2;
        int y = (height - squareSize) / 2;

        return image.getSubimage(x, y, squareSize, squareSize);
    }

    /**
     * 이미지 리사이징
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();

        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);

        graphics2D.dispose();

        return resizedImage;
    }

    /*유효성 검증*/
    private void validateFile(MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
    }

    private String generateFileName(MultipartFile file, String folder) {
        return folder + "/" + UUID.randomUUID().toString() + ".jpg";
    }

    public byte[] downloadImageFromUrl(String imageUrl) throws IOException {
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(
                    imageUrl,
                    byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new IOException("이미지 다운로드 실패: 응답 코드 " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new IOException("이미지 다운로드 중 오류 발생: " + e.getMessage(), e);
        }
    }

    public String uploadImageBytes(byte[] imageBytes, String folder) {
        if (s3Client == null) {
            log.warn("S3 client not configured. Using local storage fallback.");
            throw new RuntimeException("S3 not configured");
        }

        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("imageBytes is empty");
        }

        try {
            InputStream inputStream = new ByteArrayInputStream(imageBytes);
            BufferedImage originalImage = ImageIO.read(inputStream);

            if (originalImage == null) {
                throw new BadRequestException("Invalid image bytes");
            }

            BufferedImage squareImage = cropToSquare(originalImage);

            BufferedImage resizedImage = resizeImage(squareImage, IMAGE_SIZE, IMAGE_SIZE);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "jpg", outputStream);

            String fileName = folder + "/" + UUID.randomUUID().toString() + ".jpg";

            byte[] finalBytes = outputStream.toByteArray();
            InputStream finalInputStream = new ByteArrayInputStream(finalBytes);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/jpeg");
            metadata.setContentLength(finalBytes.length);

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    fileName,
                    finalInputStream,
                    metadata
            );

            s3Client.putObject(putObjectRequest);

            log.info("Successfully uploaded byte image to S3: {}", fileName);

            return fileName;

        } catch (IOException e) {
            log.error("Failed to upload byte image to S3", e);
            throw new RuntimeException("Failed to upload byte image", e);
        }
    }

}