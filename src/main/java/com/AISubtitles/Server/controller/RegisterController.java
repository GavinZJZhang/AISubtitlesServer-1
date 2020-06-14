package com.AISubtitles.Server.controller;

import com.AISubtitles.Server.domain.Result;
import com.AISubtitles.Server.domain.User;
import com.AISubtitles.Server.domain.UserAuths;
import com.AISubtitles.Server.service.RegistService;
import com.AISubtitles.Server.utils.Md5Utils;
import com.AISubtitles.common.CodeConsts;
import com.AISubtitles.common.StatusConsts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.sql.Date;


@RestController
public class RegisterController {

    @Autowired
    RegistService registService;

    @PostMapping(value = "user/regist/validPhone")
    public Result isExistPhoneNumber(String userPhoneNumber) {
        return registService.findByUserPhoneNumber(userPhoneNumber);
    }

    @PostMapping(value = "user/regist/validEmail")
    public Result isExistEmail(String userEmail) {
        return registService.findByUserEmail(userEmail);
    }


    @PostMapping(value = "user/regist")
    public Result handleRegist(HttpSession session, String userName,
                               String userEmail, String userPassword,
                               Date userBirthday) {
        Result result = new Result();

        try {
            User user = new User();
            user.setUserName(userName);
            user.setUserBirthday(userBirthday);
            user.setUserEmail(userEmail);
            user.setUserPhoneNumber("111");
            user.setUserGender("male");
            user.setImage("");

            UserAuths userAuths = new UserAuths();
            userAuths.setLoginType("email");
            userAuths.setUserPassword(Md5Utils.md5(userPassword));

            result = registService.regist(user, userAuths);

        } catch (Exception e) {
            result.setCode(CodeConsts.CODE_SERVER_ERROR);
            result.setData(e.getCause());
            e.printStackTrace();
        }

        return result;
    }
}
