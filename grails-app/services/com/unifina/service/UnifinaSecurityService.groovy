package com.unifina.service

import org.apache.log4j.Logger

import com.unifina.domain.security.SecUser;

class UnifinaSecurityService {
	
	def springSecurityService
	Logger log = Logger.getLogger(UnifinaSecurityService)
	
	boolean checkUser(instance) {
		if (instance.hasProperty("user") && instance.user?.id!=null) {
			boolean result = instance.user.id == springSecurityService.getCurrentUser().id
			if (!result)
				log.warn("User $springSecurityService.currentUser.id tried to access $instance owned by user $instance.user.id!")
			return result
		}
		else return true
	}
	
	boolean canAccess(instance) {
		if (instance) {
			if (!checkUser(instance)) {
				return false
			}
		}
		return true
	}
	
	SecUser checkDataToken(String username, String dataToken) {
		SecUser user = SecUser.findByUsernameAndDataToken(username,dataToken)
		if (!user)
			throw new RuntimeException("Invalid username or token")
		else return user
	}
}