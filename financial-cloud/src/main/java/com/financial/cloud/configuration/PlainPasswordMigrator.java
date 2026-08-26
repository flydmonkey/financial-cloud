package com.financial.cloud.configuration;

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.financial.cloud.domain.idm.UserInfo;
import com.financial.cloud.repository.idm.UserInfoMapper;
import com.financial.cloud.util.PlainPasswordMigration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
@RequiredArgsConstructor
@Slf4j
public class PlainPasswordMigrator implements org.springframework.beans.factory.SmartInitializingSingleton {

	private final UserInfoMapper userInfoMapper;

	private final PasswordEncoder passwordEncoder;

	@Override
	public void afterSingletonsInstantiated() {
		LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
		wrapper.likeRight(UserInfo::getPassword, "{plain}");
		List<UserInfo> users = userInfoMapper.selectList(wrapper);
		if (users.isEmpty()) {
			log.debug("No legacy {{plain}} passwords found");
			return;
		}

		for (UserInfo user : users) {
			UserInfo update = new UserInfo();
			update.setId(user.getId());
			update.setPassword(PlainPasswordMigration.migrate(user.getPassword(), passwordEncoder));
			userInfoMapper.updateById(update);
			log.warn("Migrated legacy plain password to bcrypt for user '{}'", user.getUsername());
		}
		log.info("Migrated {} user password(s) from {{plain}} to bcrypt", users.size());
	}
}
