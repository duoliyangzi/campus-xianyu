package com.campus.xianyu.upload;

import com.campus.xianyu.auth.TokenService;
import com.campus.xianyu.common.ApiResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private final Path imageUploadDir = Paths.get("uploads", "images").toAbsolutePath().normalize();
    private final TokenService tokenService;

    public UploadController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/images")
    public ApiResponse<UploadResponse> uploadImage(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        tokenService.findUserId(authorization)
                .orElseThrow(() -> new IllegalArgumentException("请先登录"));
        if (file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("图片大小不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("只能上传图片文件");
        }

        String extension = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("图片格式仅支持 jpg、jpeg、png、webp、gif");
        }

        Files.createDirectories(imageUploadDir);
        String filename = UUID.randomUUID() + "." + extension;
        Path target = imageUploadDir.resolve(filename).normalize();
        file.transferTo(target);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/images/")
                .path(filename)
                .toUriString();
        return ApiResponse.ok("图片上传成功", new UploadResponse(url));
    }

    private String extensionOf(String filename) {
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            throw new IllegalArgumentException("图片文件名缺少扩展名");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public record UploadResponse(String url) {
    }
}