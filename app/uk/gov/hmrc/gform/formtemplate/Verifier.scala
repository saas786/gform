/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.gform.core.{ FOpt, fromOptA }
import uk.gov.hmrc.gform.sharedmodel.form.Submitted
import uk.gov.hmrc.gform.sharedmodel.formtemplate._
import uk.gov.hmrc.gform.sharedmodel.formtemplate.destinations.DestinationId
import uk.gov.hmrc.gform.sharedmodel.formtemplate.destinations.Destinations.DestinationList
import uk.gov.hmrc.gform.sharedmodel.formtemplate.destinations.Destination.StateTransition
import cats.implicits._

import scala.concurrent.ExecutionContext

trait Verifier {
  def verify(formTemplate: FormTemplate)(implicit ec: ExecutionContext): FOpt[Unit] = {

    val sections = formTemplate.sections

    val exprs: List[ComponentType] = sections.flatMap(_.fields.map(_.`type`))

    val languages = formTemplate.languages

    for {
      _ <- fromOptA(FormTemplateValidator.validateLanguages(languages).toEither)
      _ <- fromOptA(FormTemplateValidator.validateRepeatingSectionFields(sections).toEither)
      _ <- fromOptA(FormTemplateValidator.validateChoiceHelpText(sections).toEither)
      _ <- fromOptA(FormTemplateValidator.validateUniqueFields(sections).toEither)
      _ <- fromOptA(DestinationsValidator.validate(formTemplate))
      _ <- fromOptA(FormTemplateValidator.validateForwardReference(sections).toEither)
      _ <- fromOptA(FormTemplateValidator.validate(exprs, formTemplate).toEither)
      _ <- fromOptA(FormTemplateValidator.validateDependencyGraph(formTemplate).toEither)
      _ <- fromOptA(FormTemplateValidator.validateEnrolmentSection(formTemplate).toEither)
      _ <- fromOptA(FormTemplateValidator.validateRegimeId(formTemplate).toEither)
      _ <- fromOptA(FormTemplateValidator.validateEmailParameter(formTemplate).toEither)
      _ <- fromOptA(FormTemplateValidator.validateEnrolmentIdentifier(formTemplate).toEither)
      _ <- fromOptA(FormTemplateValidator.validateDates(formTemplate).toEither)
      _ <- fromOptA(FormTemplateValidator.validateGroup(formTemplate).toEither)
      _ <- fromOptA(FormTemplateValidator.validateRevealingChoice(formTemplate).toEither)
      _ <- fromOptA(FormTemplateValidator.validateEmailVerification(formTemplate).toEither)
    } yield ()
  }

  def mkSpecimen(formTemplate: FormTemplate): FormTemplate =
    formTemplate.copy(
      _id = FormTemplateId("specimen-" + formTemplate._id.value),
      authConfig = Anonymous,
      sections = formTemplate.sections.map(
        removeIncludeIf _ andThen mkComponentsOptional _ andThen noValidators _ andThen noEeittExpression _),
      destinations = DestinationList(NonEmptyList.of(StateTransition(DestinationId("abc"), Submitted, "true", true)))
    )

  private def removeIncludeIf(section: Section): Section = section.copy(includeIf = None)

  private val constant1 = TextExpression(Constant("1"))

  private def mkComponentsOptional(section: Section): Section =
    section.copy(
      fields = mkOptional(section.fields),
      repeatsMax = section.repeatsMax.map(_ => constant1),
      repeatsMin = section.repeatsMin.map(_ => constant1)
    )

  private def noValidators(section: Section): Section =
    section.copy(validators = None)

  private def noEeittExpression(section: Section): Section =
    section.copy(fields = section.fields.map {
      case f @ IsText(text @ Text(_, EeittCtx(eeitt), _, _)) =>
        f.copy(`type` = text.copy(value = Constant(eeitt.toString)))
      case f @ IsTextArea(textArea @ TextArea(_, EeittCtx(eeitt), _)) =>
        f.copy(`type` = textArea.copy(value = Constant(eeitt.toString)))
      case f => f
    })

  private def mkOptional(fcs: List[FormComponent]): List[FormComponent] = fcs.map {
    case fc @ IsGroup(group) =>
      fc.copy(
        mandatory = false,
        `type` = group.copy(fields = mkOptional(group.fields))
      )
    case fc @ IsRevealingChoice(revealingChoice) =>
      fc.copy(
        mandatory = false,
        `type` = revealingChoice.copy(
          options = revealingChoice.options.map(rce => rce.copy(revealingFields = mkOptional(rce.revealingFields))))
      )
    case fc => fc.copy(mandatory = false)
  }

}
