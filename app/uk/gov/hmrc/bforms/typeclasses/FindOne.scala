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

package uk.gov.hmrc.bforms.typeclasses

import play.api.libs.json.{ JsObject, Json }
import uk.gov.hmrc.bforms.model.{ FormTemplate, SaveAndRetrieve, Schema }
import uk.gov.hmrc.bforms.repositories.{ FormTemplateRepository, SaveAndRetrieveRepository, SchemaRepository }

import scala.concurrent.{ ExecutionContext, Future }

trait FindOne[T] {
  def apply(selector: JsObject)(implicit ex: ExecutionContext): Future[Option[T]]
}

object FindOne {
  implicit def schema(implicit repo: SchemaRepository) = new FindOne[Schema] {
    def apply(selector: JsObject)(implicit ex: ExecutionContext): Future[Option[Schema]] = {
      repo.findOne(selector, Json.obj())
    }
  }

  implicit def formTemplate(implicit repo: FormTemplateRepository) = new FindOne[FormTemplate] {
    def apply(selector: JsObject)(implicit ex: ExecutionContext): Future[Option[FormTemplate]] = {
      repo.findOne(selector, Json.obj())
    }
  }

  implicit def saveAndRetrieve(implicit repo: SaveAndRetrieveRepository) = new FindOne[SaveAndRetrieve] {
    def apply(selector: JsObject)(implicit ex: ExecutionContext): Future[Option[SaveAndRetrieve]] = {
      repo.retrieve(selector)
    }
  }
}
