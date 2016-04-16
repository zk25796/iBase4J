package org.ibase4j.core.interceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ibase4j.core.support.HttpCode;
import org.ibase4j.core.util.WebUtil;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * 登录拦截器
 * 
 * @author ShenHuaJie
 * @version 2016年4月2日 下午4:19:47
 */
public class HandlerInterceptor extends HandlerInterceptorAdapter {
	private static Logger logger = LogManager.getLogger(HandlerInterceptor.class);

	private static String[] notFilter = new String[] {};
	static {
		try {
			String url = "";
			Properties properties = PropertiesLoaderUtils.loadAllProperties("classpath:config/whiteURL.properties");
			for (Enumeration<?> iterator = properties.keys(); iterator.hasMoreElements();) {
				String key = (String) iterator.nextElement();
				url += "," + properties.getProperty(key) + ",";
			}
			notFilter = url.split(",");
		} catch (IOException e) {
			logger.error("", e);
		}
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		boolean success = true;
		String reqUri = request.getRequestURI();
		String url = request.getServletPath();
		logger.info(url);
		for (String s : notFilter) { // 如果uri中包含不过滤的uri，则不进行过滤
			if (reqUri.indexOf(s) != -1 && StringUtils.isNotBlank(s)) {
				return success;
			}
		}
		String curruser = WebUtil.getCurrentUser(request);
		if (reqUri.indexOf("online") == -1) { // 后端请求后端数据
			if (curruser == null) {
				success = false;
				response.setStatus(HttpCode.HTTP_CODE_401);
			}
		} else { // 前端请求后端数据
			if (curruser == null) {
				success = false;
				String isAjax = request.getHeader("x-requested-with");
				if (StringUtils.isNotEmpty(isAjax)) {
					response.setStatus(HttpCode.HTTP_CODE_401);
				} else {
					String host = InetAddress.getLocalHost().getHostAddress();
					String redirect = "/home.html#/login";
					if (host != null && host.startsWith("192.168.1")) {
						redirect = "/";
					}
					PrintWriter writer = response.getWriter();
					writer.write("<script type=\"text/javascript\">window.location.href='" + redirect + "'</script>");
					writer.flush();
					writer.close();
				}
			}
		}
		if (!success)
			logger.info("Interceptor:" + HttpCode.MSG.get(response.getStatus()));
		return success;
	}
}
