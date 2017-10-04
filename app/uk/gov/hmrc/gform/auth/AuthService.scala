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

package uk.gov.hmrc.gform.auth

import play.api.Logger
import play.api.libs.json.{ JsString, Json }
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.gform.core.FOpt

import scala.concurrent.Future

class AuthService(
    authRepository: AuthRepository
) {

  def whiteListed(email: String): Future[Option[WhiteListedUser]] = authRepository.find(email)

  def insert(email: String): Future[WriteResult] = authRepository.insert(WhiteListedUser(_id = email))

  def delete(email: String): FOpt[Unit] = authRepository.delete(email)

  def all(): Future[List[WhiteListedUser]] = authRepository.search(Json.obj())

}