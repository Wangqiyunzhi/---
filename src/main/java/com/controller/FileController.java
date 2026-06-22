package com.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.annotation.IgnoreAuth;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.entity.ConfigEntity;
import com.entity.EIException;
import com.service.ConfigService;
import com.utils.R;

@RestController
@RequestMapping("file")
@SuppressWarnings({"unchecked","rawtypes"})
public class FileController {

	@Autowired
	private ConfigService configService;

	@Value("${file.upload-dir:${app.upload-dir:C:/w001/weixin_upload}}")
	private String uploadDir;

	/**
	 * 上传文件
	 * 返回：
	 *  - file: 文件名
	 *  - url:  可直接访问的静态资源地址：{host}{ctx}/upload/{file}
	 */
	@PostMapping("/upload")
	public R upload(@RequestParam("file") MultipartFile file, String type, HttpServletRequest request) throws Exception {
		if (file == null || file.isEmpty()) {
			throw new EIException("上传文件不能为空");
		}

		String original = file.getOriginalFilename();
		String fileExt = "png";
		if (original != null && original.contains(".")) {
			fileExt = original.substring(original.lastIndexOf(".") + 1);
		}

		String fileName = new Date().getTime() + "." + fileExt;

		Path dir = Paths.get(uploadDir);
		Files.createDirectories(dir);

		Path target = dir.resolve(fileName);
		file.transferTo(target.toFile());

		if (StringUtils.isNotBlank(type) && "1".equals(type)) {
			ConfigEntity configEntity = configService.selectOne(new EntityWrapper<ConfigEntity>().eq("name", "faceFile"));
			if (configEntity == null) {
				configEntity = new ConfigEntity();
				configEntity.setName("faceFile");
				configEntity.setValue(fileName);
			} else {
				configEntity.setValue(fileName);
			}
			configService.insertOrUpdate(configEntity);
		}

		String base = resolvePublicAssetBase(request);
		String url = base + "/upload/" + fileName;

		return R.ok().put("file", fileName).put("url", url);
	}

	private String resolvePublicAssetBase(HttpServletRequest request) {
		String configuredBase = getConfigValue("asset_public_base");
		if (StringUtils.isBlank(configuredBase)) {
			configuredBase = getConfigValue("asset_cdn_base");
		}
		if (StringUtils.isNotBlank(configuredBase)) {
			String normalized = configuredBase.trim().replaceAll("/+$", "");
			if (normalized.endsWith("/upload")) {
				normalized = normalized.substring(0, normalized.length() - "/upload".length());
			}
			return normalized;
		}
		return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
				+ request.getContextPath();
	}

	private String getConfigValue(String name) {
		ConfigEntity config = configService.selectOne(new EntityWrapper<ConfigEntity>().eq("name", name));
		return config == null ? "" : StringUtils.trimToEmpty(config.getValue());
	}

	/**
	 * view/download 你可以留着（兼容旧数据），但以后不要再让前端存 file/view
	 */
	@IgnoreAuth
	@GetMapping("/view")
	public void view(@RequestParam("fileName") String fileName, HttpServletResponse response) {
		try {
			File file = Paths.get(uploadDir).resolve(fileName).toFile();
			if (!file.exists()) {
				response.setStatus(404);
				return;
			}
			String contentType = Files.probeContentType(file.toPath());
			if (contentType == null) contentType = "application/octet-stream";

			response.reset();
			response.setContentType(contentType);
			response.setHeader("Cache-Control", "max-age=86400");

			Files.copy(file.toPath(), response.getOutputStream());
			response.getOutputStream().flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@IgnoreAuth
	@GetMapping("/download")
	public void download(@RequestParam("fileName") String fileName, HttpServletResponse response) {
		try {
			File file = Paths.get(uploadDir).resolve(fileName).toFile();
			if (!file.exists()) {
				response.setStatus(404);
				return;
			}

			response.reset();
			response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
			response.setHeader("Cache-Control", "no-cache");
			response.setContentType("application/octet-stream");

			Files.copy(file.toPath(), response.getOutputStream());
			response.getOutputStream().flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
