import geb.spock.GebReportingSpec

import org.openqa.selenium.Cookie

import spock.lang.Shared
import core.pages.CanvasPage
import core.pages.LoginPage

class LoginSpec extends GebReportingSpec {
	
	def "cannot log in with empty form"() {
		when: "just clicked to log in"
		to LoginPage
		loginButton.click()
		then: "should not go forward"
		at LoginPage
		$("p.login-failed-message", text:"Sorry, we were not able to find a user with that username and password.").displayed
	}
	
	def "cannot log in with false information"() {
		when: "given false username and password"
		to LoginPage
		username = "falseUserName"
		password = "falsePassword"
		loginButton.click()
		then: "should not go forward"
		at LoginPage
		$("p.login-failed-message", text:"Sorry, we were not able to find a user with that username and password.").displayed
	}
	
	def "login without remember me works"(){
		when: "logged in"
		to LoginPage
		username = "tester1@streamr.com"
		password = "tester1TESTER1"
		loginButton.click()
		then: 
		at CanvasPage
	}
	
	@Shared cookieName = "streamr_remember_me"
	@Shared cookieValue = "initial"
	
	def "log in with checking remember me"(){
		when: "logged in and clicked remember me"
		to LoginPage
		username = "tester1@streamr.com"
		password = "tester1TESTER1"
		$(".checkbox-inline").click()
		loginButton.click()
		waitFor {
			at CanvasPage
		}
		cookieValue = driver.manage().getCookieNamed(cookieName).getValue()
		then:
		at CanvasPage
	}
	
	def "the browser should remember me"(){
		Cookie cookie = new Cookie(cookieName, cookieValue)
		driver.manage().addCookie(cookie)
		expect: "should be able to go straight to the canvas page"
		to CanvasPage
	}
	
	
}
