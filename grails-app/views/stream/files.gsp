<g:if test="${feedFiles?.size()>0}">
	<p>This stream has the following history available:</p>
	
	<table class="table table-striped table-bordered table-hover table-condensed table-responsive">
		<thead>
			<tr>
				<td>Begin Date</td>
				<td>End Date</td>
			</tr>
		</thead>
		<tbody>
			<g:each in="${feedFiles}">
				<tr>
					<td><g:formatDate date="${it.beginDate}" timeZone="UTC" format="${message(code:'default.dateOnly.format')}"/></td>
					<td><g:formatDate date="${it.endDate}" timeZone="UTC" format="${message(code:'default.dateOnly.format')}"/></td>
				</tr>
			</g:each>
		</tbody>
	</table>
</g:if>
<g:else>
	<p>This stream has no history.</p>
</g:else>