package cn.e3mall.order.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import cn.e3mall.cart.service.CartService;
import cn.e3mall.common.util.CookieUtils;
import cn.e3mall.common.util.E3Result;
import cn.e3mall.common.util.JsonUtils;
import cn.e3mall.pojo.TbItem;
import cn.e3mall.pojo.TbUser;
import cn.e3mall.sso.service.TokenService;

/**
 * 登录拦截器
 * <p>Title: LoginInterceptor</p>
 * <p>Description: </p>
 * @version 1.0
 */
public class LoginInterceptor implements HandlerInterceptor{
	
	@Value("${SSO_URL}")
	private String SSO_URL;
	@Autowired
	private TokenService tokenService;
	@Autowired
	private CartService cartService;
	
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		//从cookie中取token
		String token = CookieUtils.getCookieValue(request, "token");
		//判断token是否存在
		if (StringUtils.isBlank(token)) {
			//如果token不存在 则为未登录状态 跳转到sso系统的登录页面。用户登录成功后 跳转到当前请求的url
			response.sendRedirect(SSO_URL+"/page/login?redirect="+request.getRequestURL());
			//拦截
			return false;
		}
		//如果token存在 根据token取用户信息
		E3Result e3Result = tokenService.getUserByToken(token);
		//如果取不到用户信息 说明用户登录已经过期 需要重新登录 
		if (e3Result.getStatus()!=200) {
			//跳转到sso系统的登录页面。用户登录成功后 跳转到当前请求的url
			response.sendRedirect(SSO_URL+"/page/login?redirect="+request.getRequestURL());
			//拦截
			return false;
		}
		//如果取到用户信息 说明已经登录 把用户信息写入request
		TbUser user = (TbUser) e3Result.getData();
		request.setAttribute("user", user);
		//判断cookie中是否有购物车数据 如果有合并到服务端
		String jsonCartList = CookieUtils.getCookieValue(request, "cart",true);
		if (StringUtils.isNotBlank(jsonCartList)) {
			//合并购物车
			cartService.mergeCart(user.getId(), JsonUtils.jsonToList(jsonCartList, TbItem.class));
			//把cookie中的购物车清空
			CookieUtils.deleteCookie(request, response, "cart");
		}
		//放行
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, Exception arg3)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postHandle(HttpServletRequest arg0, HttpServletResponse arg1, Object arg2, ModelAndView arg3)
			throws Exception {
		// TODO Auto-generated method stub
		
	}

}
