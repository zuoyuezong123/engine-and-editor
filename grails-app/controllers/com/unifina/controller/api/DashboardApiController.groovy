package com.unifina.controller.api

import com.unifina.api.SaveDashboardCommand
import com.unifina.api.ValidationException
import com.unifina.domain.dashboard.Dashboard
import com.unifina.domain.security.SecUser
import com.unifina.security.StreamrApi
import com.unifina.service.DashboardService
import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured

@Secured(["IS_AUTHENTICATED_ANONYMOUSLY"])
class DashboardApiController {
	DashboardService dashboardService

	@StreamrApi
	def index() {
		def dashboards = dashboardService.findAllDashboards((SecUser) request.apiUser)
		render(dashboards*.toSummaryMap() as JSON)
	}

	@StreamrApi
	def show(Long id) {
		def dashboard = dashboardService.findById(id, (SecUser) request.apiUser)
		render(dashboard.toMap() as JSON)
	}

	@StreamrApi
	def save(SaveDashboardCommand command) {
		if (!command.validate()) {
			throw new ValidationException(command.errors)
		}
		def dashboard = new Dashboard(
			name: command.name,
			user: request.apiUser
		)
		dashboard.save(failOnError: true, validate: true)
		render(dashboard.toMap() as JSON)
	}

	@StreamrApi
	def update(Long id, SaveDashboardCommand command) {
		if (!command.validate()) {
			throw new ValidationException(command.errors)
		}
		def dashboard = dashboardService.update(id, command, (SecUser) request.apiUser)
		render(dashboard.toMap() as JSON)
	}

	@StreamrApi
	def delete(Long id) {
		dashboardService.deleteById(id, (SecUser) request.apiUser)
		render(status: 204)
	}
}