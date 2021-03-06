package com.AISubtitles.Server.service.impl;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.AISubtitles.Server.dao.UserAuthsDao;
//import com.AISubtitles.Server.dao.FindPasswordDao;
import com.AISubtitles.Server.dao.UserDao;

import com.AISubtitles.Server.domain.User;
import com.AISubtitles.Server.domain.UserAuths;
import com.AISubtitles.Server.service.FindPasswordService;
import com.AISubtitles.Server.service.TokenService;
import com.AISubtitles.Server.utils.Md5Utils;
import com.AISubtitles.Server.utils.TokenUtil;
import com.AISubtitles.common.CodeConsts;
import com.AISubtitles.Server.domain.Result;

/**
 * @author Gavin
 * @desc FindPasswordServiceImpl
 */
@Service
public class FindPasswordServiceImpl implements FindPasswordService {

	@Autowired
	private UserDao userDao;

	@Autowired
	private UserAuthsDao userAuthsDao;

	@Autowired
	private TokenService tokenService;


	/**
	 * 修改用户密码
	 */
	@Override
	public Result update(String newPassword) {
		Result resultUser = new Result();
		int userId = TokenUtil.getTokenUserId();
		System.out.println(userId);
		Integer success = userAuthsDao.update(Md5Utils.md5(newPassword), userId);
		if (success > 0) {
			resultUser.setCode(CodeConsts.CODE_SUCCESS);
		}
		return resultUser;
	}

	@Override
	public Result validateEmailCode(String emailCode, HttpServletResponse response, HttpSession session) {
		Result resultUser = new Result();
		String code = (String) session.getAttribute("code");
		if (code.equals(emailCode)) {
			resultUser.setCode(CodeConsts.CODE_SUCCESS);
			resultUser.setData(null);

			String email = (String) session.getAttribute("userEmail");
			User user = userDao.findByUserEmail(email);
			if(user == null) {
				resultUser.setCode(CodeConsts.CODE_ACCOUNT_NOT_EXIST);
				resultUser.setData("您录入的账号不存在！");
				return resultUser;	
			}
			UserAuths userAuths = userAuthsDao.findByUserId(userDao.findByUserEmail(email).getUserId());
			String token = tokenService.getToken(userAuths);
            Cookie cookie = new Cookie("token", token);
            cookie.setPath("/");
            response.addCookie(cookie);

			return resultUser;
		} else {
			resultUser.setCode(CodeConsts.CODE_RECOVER_PASSWORD_ERROR);
			resultUser.setData("验证码错误");
			return resultUser;
		}

	}

	/**
	 * 验证短信验证码
	 */
	@Override
	public Result validateSMSCode(String SMSCode, HttpSession session) {
		Result resultUser = new Result();
		String code = (String) session.getAttribute("code");
		if (code.equals(SMSCode)) {
			resultUser.setCode(200);
			resultUser.setData("");
			return resultUser;
		} else {
			resultUser.setCode(606);
			resultUser.setData("验证码错误");
			return resultUser;
		}

	}

	/**
	 * 查询用户信息
	 */
	@Override
	public Result select(String accountNum) {
		Result resultUser = new Result();
		//User user = new User();
		//User userE = new User();
		//User userEN = new User();
		//User userE = userDao.findByUserEmail(accountNum);
		User userEN = userDao.findByUserEmailOrUserPhoneNumber(accountNum, accountNum);
		int countuEmail = userDao.countByUserEmail(accountNum);
		int countuPhoneNumber = userDao.countByUserPhoneNumber(accountNum);
		if (countuEmail > 0 || countuPhoneNumber > 0) {
			resultUser.setCode(CodeConsts.CODE_SUCCESS);
			resultUser.setData(userEN);
			return resultUser;
		} else {
			resultUser.setCode(CodeConsts.CODE_ACCOUNT_NOT_EXIST);
			resultUser.setData("您录入的账号不存在！");
			return resultUser;
		}
	}
}
