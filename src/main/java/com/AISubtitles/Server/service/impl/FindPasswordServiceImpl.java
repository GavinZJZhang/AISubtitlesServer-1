package com.AISubtitles.Server.service.impl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//import com.AISubtitles.Server.dao.FindPasswordDao;
import com.AISubtitles.Server.dao.UserDao;

import com.AISubtitles.Server.domain.User;
import com.AISubtitles.Server.service.FindPasswordService;
import com.AISubtitles.Server.domain.Result;

/**
 * @author Gavin
 * @desc FindPasswordServiceImpl
 */
@Service
public class FindPasswordServiceImpl implements FindPasswordService {

	@Autowired
	private UserDao userDao;

	User user = new User();

	/**
	 * 修改用户密码
	 */
	@Override
	public Result<User> update(String password, String newpassword) {
		Result<User> resultUser = new Result<>();
		if (password.equals(newpassword)) {
			Integer success = userDao.update(newpassword, user.getUserEmail());
			if (success > 0) {
				resultUser.setCode(200);
			}
			return resultUser;
		} else {
			resultUser.setCode(608);
			resultUser.setMessage("两次密码输入的不一致！");
			return resultUser;
		}
	}

	@Override
	public Result<User> validateCode(String emailCode, HttpSession session) {
		Result<User> resultUser = new Result<>();
		String code = (String) session.getAttribute("code");
		if (code.equals(emailCode)) {
			resultUser.setCode(200);
			resultUser.setMessage("");
			return resultUser;
		} else {
			resultUser.setCode(606);
			resultUser.setMessage("验证码错误");
			return resultUser;
		}

	}

	/**
	 * 查询用户信息
	 */
	@Override
	public Result<User> select(HttpServletRequest request) {
		Result<User> resultUser = new Result<>();
		String accountnum = request.getParameter("accountnum");
		
		user = userDao.findByUserName(accountnum);
		int account = userDao.countByUserName(accountnum);
		if (account > 0) {
			resultUser.setCode(200);
			resultUser.setData(user);
			return resultUser;
		} else {
			resultUser.setCode(604);
			resultUser.setMessage("您录入的账号不存在！");
			return resultUser;
		}
	}
}