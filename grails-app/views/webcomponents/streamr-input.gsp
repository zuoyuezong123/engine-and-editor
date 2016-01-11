<link rel="import" href="${createLink(uri:"/webcomponents/polymer.html", plugin:"unifina-core")}">


<polymer-element name="streamr-input" extends="streamr-widget">
    <template>
        <shadow></shadow>
    </template>

    <script>
        Polymer('streamr-input', {
            ready: function() {
                var _this = this
                this.bindEvents(_this.$["streamr-widget-container"])

                this.getModuleJson(function(json) {
                    var resendOptions = _this.getResendOptions(json)
                    _this.widget = _this.createWidget(json)
                    $(_this.widget).on("input", function(e, value) {
                        _this.sendRequest({
                            type: "uiEvent",
                            value: value
                        })
                    })

                    _this.subscribe(
                        function(message) {
                            if(_this.widget && _this.widget.receiveResponse)
                                _this.widget.receiveResponse(message)
                        },
                        resendOptions
                    )
                })

            },
            <g:if test="${params.lightDOM}">
            parseDeclaration: function(elementElement) {
                return this.lightFromTemplate(this.fetchTemplate(elementElement))
            }
            </g:if>
        });
    </script>
</polymer-element>