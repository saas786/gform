/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.gform.bank_account_reputation

import org.slf4j.LoggerFactory
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.gform.auditing.loggingHelpers
import uk.gov.hmrc.gform.sharedmodel.Account
import uk.gov.hmrc.gform.wshttp.WSHttp
import uk.gov.hmrc.http.{ HeaderCarrier, LowPriorityHttpReadsJson }

import scala.concurrent.{ ExecutionContext, Future }

trait BankAccountReputationAlgebra[F[_]] {
  def exists(account: Account)(implicit hc: HeaderCarrier): F[Response]
}

class BankAccountReputationConnector(wSHttp: WSHttp, baseUrl: String)(implicit ec: ExecutionContext)
    extends BankAccountReputationAlgebra[Future] with LowPriorityHttpReadsJson {
  private val logger = LoggerFactory.getLogger(getClass)

  def exists(account: Account)(implicit hc: HeaderCarrier): Future[Response] = {
    logger.info(s"Check if bank account exists, headers: '${loggingHelpers.cleanHeaderCarrierHeader(hc)}'")
    wSHttp.POST[Account, Response](s"$baseUrl/modcheck", account)
  }
}

case class Response(accountNumberWithSortCodeIsValid: Boolean, nonStandardAccountDetailsRequiredForBacs: String)

object Response {
  private val reads = Reads[Response] { json =>
    (json \ "parameters" \ "nonStandardAccountDetailsRequiredForBacs").asOpt[String] match {
      case Some(str) => parse(json, str)
      case None =>
        JsError(
          "the response does not match desired parameters : accountNumberWithSortCodeIsValid, accountNumberWithSortCodeIsValid")
    }
  }

  private def parse(json: JsValue, str: String) =
    str match {
      case "no" | "yes" | "inapplicable" =>
        JsSuccess(Response((json \ "accountNumberWithSortCodeIsValid").as[Boolean], str))
      case _ => JsError("Response did not match no, yes, inapplicable")
    }

  private val basic: OFormat[Response] = Json.format[Response]

  private val readsAll = (basic: Reads[Response]) | reads
  private val writes: OWrites[Response] = basic

  implicit val format: OFormat[Response] = OFormat(readsAll, writes)
}
