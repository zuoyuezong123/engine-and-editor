package com.unifina.signalpath.remote

import com.unifina.datasource.DataSource
import com.unifina.datasource.DataSourceEventQueue
import com.unifina.utils.Globals
import com.unifina.utils.testutils.ModuleTestHelper
import groovy.json.JsonBuilder
import org.apache.http.Header
import org.apache.http.HttpResponse
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.concurrent.FutureCallback
import org.apache.http.entity.StringEntity
import org.apache.http.nio.client.HttpAsyncClient
import spock.lang.Specification

class HttpSpec extends Specification {
	Http module
	boolean isAsync = true
	String dummyHeaderName = "x-unit-test"
	String dummyHeaderValue = "testing123"
	def dummyHeader = [(dummyHeaderName): dummyHeaderValue]

	/**
	 * Module input name -> List of values for each iteration
	 */
	Map<String, List> inputs = [
		params: [[:], [:], [:]],
		headers: [[:], [:], [:]],
		body: [[:], [:], [:]]
	]

	/**
	 * Override "response" to provide the mock server implementation
	 * If closure, will be executed (argument is HttpUriRequest)
	 * If constant, will be returned
	 * If array, elements will be returned in sequence (closures executed, cyclically repeated if too short)
	 * If you want to return an array,
	 *   use closure that returns an array (see default below)
	 *   or array of arrays
	 */
	def response = { request -> [] }

	Map<String, List> outputs = [
		data: [[], [], []],
		errors: [[], [], []],
		statusCode: [200d, 200d, 200d],
		//ping: [0, 0, 0],
		headers: [dummyHeader, dummyHeader, dummyHeader]
	]

	def setup() {
		// TestableHttp is Http module wrapped so that we can inject our own mock HttpClient
		// Separate class is needed in same path as Http.java; anonymous class won't work with de-serializer
		TestableHttp.httpClient = mockClient
		module = new TestableHttp()
		module.init()
		module.configure([
			params : [
				[name: "URL", value: "localhost"],
				[name: "verb", value: "GET"],
			]
		])
	}

	private boolean test() {
		return new ModuleTestHelper.Builder(module, inputs, outputs)
			.overrideGlobals { mockGlobals }
			.onModuleInstanceChange { newInstance -> module = newInstance }
			.test()
	}

	/** Mocked event queue. Works manually in tests, please call module.receive(queuedEvent) */
	def mockGlobals = Stub(Globals) {
		getDataSource() >> Stub(DataSource) {
			getEventQueue() >> Stub(DataSourceEventQueue) {
				enqueue(_) >> { feedEvent ->
					transaction = feedEvent.content[0]
				}
			}
		}
	}

	// temporary storage for async transaction generated by AbstractHttpModule, passing from globals to mockClient
	AbstractHttpModule.HttpTransaction transaction

	/** HttpClient that generates mock responses to HttpUriRequests according to this.response */
	def mockClient = Stub(HttpAsyncClient) {
		def responseI = [].iterator()
		execute(_, _) >> { HttpUriRequest request, FutureCallback<HttpResponse> future ->
			def mockHttpResponse = Stub(CloseableHttpResponse) {
				getEntity() >> {
					def ret = response
					// array => iterate
					if (ret instanceof Iterable) {
						// end of array -> restart from beginning
						if (!responseI.hasNext()) {
							responseI = response.iterator()
						}
						ret = responseI.hasNext() ? responseI.next() : []
					}
					// closure => execute
					if (ret instanceof Closure) {
						ret = ret(request)
					}
					// wrap mock response object in JSON and HttpEntity
					return new StringEntity(new JsonBuilder(ret).toString())
				}
				getStatusLine() >> Stub(StatusLine) {
					getStatusCode() >> 200
				}
				getAllHeaders() >> [Stub(Header) {
					getName() >> dummyHeaderName
					getValue() >> dummyHeaderValue
				}]
			}
			// synchronized requests sendOutput here already
			future.completed(mockHttpResponse)

			// simulate AbstractHttpModule.receive, but without propagation
			if (isAsync) {
				module.sendOutput(transaction)
			}
		}
	}

	void "no input, varying response, async"() {
		response = [[test: 1], [test: 2], [test: 3]]
		outputs.data = response
		expect:
		test()
	}

	void "no input, varying response, blocking"() {
		isAsync = false
		module.configure([
			options: [syncMode: [value: "sync"]]
		])
		response = [[test: 1], [test: 2], [test: 3]]
		outputs.data = response
		expect:
		test()
	}

	void "query parameters appear correctly in URL"() {
		inputs.params = [[cake: 1, is: true, or: "lie"]] * 3
		response = { HttpUriRequest request ->
			return request.URI.toString().equals("localhost?cake=1&is=true&or=lie")
		}
		outputs.data = [true, true, true]
		expect:
		test()
	}

	void "headers are added correctly to request"() {
		Map headers = [header1: "head", header2: true, header3: 3]
		inputs.headers = [headers] * 3
		response = { HttpUriRequest request ->
			int found = 0
			request.allHeaders.each { Header h ->
				if (headers.containsKey(h.name)) {
					assert headers[h.name].toString() == h.value
					found++
				}
			}
			return found
		}
		outputs.data = [3, 3, 3]
		expect:
		test()
	}

	void "timeout is sent to errors"() {
		mockClient = Stub(HttpAsyncClient) {
			execute(_, _) >> { HttpUriRequest request, FutureCallback<HttpResponse> future ->
				future.failed(new java.net.SocketTimeoutException())
				module.sendOutput(transaction)
			}
		}
		outputs.errors = [["Sending HTTP Request failed", "java.net.SocketTimeoutException"]] * 3
	}
}