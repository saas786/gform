/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.gform.auditing

import akka.stream.Materializer
import play.api.{ Configuration, Environment }
import play.api.inject.ApplicationLifecycle
import play.api.libs.ws.WSRequest
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc.Headers
import uk.gov.hmrc.gform.akka.AkkaModule
import uk.gov.hmrc.gform.config.ConfigModule
import uk.gov.hmrc.gform.playcomponents.PlayComponents
import uk.gov.hmrc.gform.wshttp.WSHttp
import uk.gov.hmrc.play.audit.filters.AuditFilter
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.config.{ AuditingConfig, LoadAuditingConfig }
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.hooks.HttpHook

class AuditingModule(configModule: ConfigModule, akkaModule: AkkaModule, playComponents: PlayComponents) { self =>

  val auditConnector: AuditConnector = new AuditConnector {

    //WARN: LoadAuditingConfig uses play deprecations.
    //Thus you can not instantiate this class if play application is not running
    override def auditingConfig: AuditingConfig = LoadAuditingConfig(s"auditing")

    //WARN! Since core libraries are using deprecated play.api.libs.ws.WS we need to provide our own non-deprecated and manually wired implementation here
    override def buildRequest(url: String)(implicit hc: HeaderCarrier): WSRequest = {
      playComponents.ahcWSComponents.wsApi.url(url).withHeaders(hc.headers: _*)
    }
  }

  val httpAuditing: HttpAuditing = new HttpAuditing {
    override def auditConnector: AuditConnector = self.auditConnector
    override def appName: String = configModule.appConfig.appName
  }

  val httpAuditingHook: HttpHook = httpAuditing.AuditingHook

  val microserviceAuditFilter = new AuditFilter {
    override val appName: String = configModule.appConfig.appName
    override def mat: Materializer = akkaModule.materializer
    override val auditConnector: AuditConnector = self.auditConnector
    override def controllerNeedsAuditing(controllerName: String): Boolean = configModule.controllerConfig.paramsForController(controllerName).needsAuditing
  }

}
object loggingHelpers {
  def cleanHeaders(headers: Headers) = s", headers: '${headers.remove("Authorization", "token", "customerId")}'"
  def cleanHeaderCarrierHeader(hc: HeaderCarrier): String = s"headers: ' ${hc.sessionId} ${hc.deviceID} ${hc.requestId} ${hc.requestChain}'"
}
