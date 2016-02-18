import core.LoginTester1Spec
import core.mixins.ConfirmationMixin
import core.mixins.DashboardMixin
import core.pages.*
import pages.*

class InputModuleDashboardSpec extends LoginTester1Spec {

	static String liveCanvasName = "InputModuleDashboardSpec"
	static String dashboardName = "test" + new Date().getTime()

	def setupSpec() {
		// @Mixin is buggy, use runtime mixins instead
		this.class.metaClass.mixin(ConfirmationMixin)
		this.class.metaClass.mixin(DashboardMixin)

		super.login()
		waitFor { at CanvasPage }

		// Go start the RunningSignalPath related to this spec
		to LiveListPage
		waitFor { at LiveListPage }
		$(".table .td", text: liveCanvasName).click()
		waitFor { at LiveShowPage }
		if (stopButton.displayed) {
			stopButton.click()
			waitForConfirmation()
			acceptConfirmation()
			waitFor { startButton.displayed }
		}

		dropDownButton.click()
		clearAndStartButton.click()
		waitForConfirmation()
		acceptConfirmation()
		waitFor { stopButton.displayed }

		createDashboard(dashboardName)

		addDashboardItem(liveCanvasName, "Table")

		save()

		waitFor {
			!$(".ui-pnotify").displayed
		}

		navbar.navSettingsLink.click()
		navbar.navLogoutLink.click()
	}

	def setup() {
		openDashboard()
	}

	def openDashboard() {
		to DashboardListPage
		$(".clickable-table a span", text: dashboardName).click()
		waitFor {
			at DashboardShowPage
		}
		if ($("body.editing").size() == 0) {
			$("#main-menu-toggle").click()
			waitFor {
				saveButton.displayed
			}
		}
	}

	def cleanup() {
		save()
	}

	def cleanupSpec() {
		// Delete the dashboard
		super.login()
		deleteDashboard(dashboardName)
	}

	void "the button works"() {
		def button
		when: "Button added"
		addDashboardItem(liveCanvasName, "Button")
		button = findDashboardItem("Button").find("button.button-module-button")
		then: "The name of the button is buttonTest"
		button.text() == "buttonTest"

		when: "Button clicked"
		button.click()
		then: "The table gets the message"
		waitFor {
			$("table.event-table-module-content tbody tr").size() == 1
			$("table.event-table-module-content tbody tr td", text: "10.0").displayed
		}
	}

	void "the textField works"() {
		def textField
		def sendBtn
		when: "TextField added"
		addDashboardItem(liveCanvasName, "TextField")
		textField = findDashboardItem("TextField").find("textarea")
		sendBtn = findDashboardItem("TextField").find(".btn.send-btn")
		then: "The text in the textField is textFieldTest"
		// Geb's own .text() didn't work for some reason
		js.exec("return \$('streamr-text-field textarea').val()") == "textFieldTest"

		when: "Text changed and sendButton clicked"
		textField << "2"
		sendBtn.click()
		then: "The table gets the message"
		waitFor {
			$("table.event-table-module-content tbody tr td", text: "textFieldTest2").displayed
		}
	}

	void "the switcher works"() {
		def switcher
		addDashboardItem(liveCanvasName, "Switcher")
		switcher = findDashboardItem("Switcher").find("div.switcher div.switcher-inner")

		when: "Switcher clicked"
		switcher.click()
		then: "The table gets the message"
		waitFor {
			$("table.event-table-module-content tbody tr td", text: "1.0").displayed
		}

		when: "Logged out and reloaded"
		save()
		waitFor {
			!$(".ui-pnotify").displayed
		}

		navbar.navSettingsLink.click()
		navbar.navLogoutLink.click()
		super.login()
		openDashboard()

		then: "Switcher remembers its state"
		waitFor {
			$(".switcher.checked").size() == 1
		}
	}
}