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

package uk.gov.hmrc.gform.formtemplate

import cats.data.NonEmptyList
import uk.gov.hmrc.gform.sharedmodel.formtemplate.{ FormComponent, Page, Section }

object SectionHelper {
  def pages(section: Section): NonEmptyList[Page] =
    section match {
      case s: Section.NonRepeatingPage => NonEmptyList.of(s.page)
      case s: Section.RepeatingPage    => NonEmptyList.of(s.page)
      case s: Section.AddToList        => s.pages
    }

  def pages(sections: List[Section]): List[Page] =
    sections.flatMap(pages(_).toList)

  def addToListRepeater(section: Section): Option[FormComponent] =
    section match {
      case s: Section.AddToList => Some(s.addAnotherQuestion)
      case _                    => None
    }

  def addToListRepeaters(sections: List[Section]): List[FormComponent] =
    sections.flatMap(addToListRepeater(_))
}
