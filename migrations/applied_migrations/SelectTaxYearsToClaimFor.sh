#!/bin/bash

echo ""
echo "Applying migration SelectTaxYearsToClaimFor"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /selectTaxYearsToClaimFor                        controllers.SelectTaxYearsToClaimForController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /selectTaxYearsToClaimFor                        controllers.SelectTaxYearsToClaimForController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeSelectTaxYearsToClaimFor                  controllers.SelectTaxYearsToClaimForController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeSelectTaxYearsToClaimFor                  controllers.SelectTaxYearsToClaimForController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "selectTaxYearsToClaimFor.title = selectTaxYearsToClaimFor" >> ../conf/messages.en
echo "selectTaxYearsToClaimFor.heading = selectTaxYearsToClaimFor" >> ../conf/messages.en
echo "selectTaxYearsToClaimFor.option1 = Option 1" >> ../conf/messages.en
echo "selectTaxYearsToClaimFor.option2 = Option 2" >> ../conf/messages.en
echo "selectTaxYearsToClaimFor.checkYourAnswersLabel = selectTaxYearsToClaimFor" >> ../conf/messages.en
echo "selectTaxYearsToClaimFor.error.required = Select selectTaxYearsToClaimFor" >> ../conf/messages.en

echo "Adding to UserAnswersEntryGenerators"
awk '/trait UserAnswersEntryGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitrarySelectTaxYearsToClaimForUserAnswersEntry: Arbitrary[(SelectTaxYearsToClaimForPage.type, JsValue)] =";\
    print "    Arbitrary {";\
    print "      for {";\
    print "        page  <- arbitrary[SelectTaxYearsToClaimForPage.type]";\
    print "        value <- arbitrary[SelectTaxYearsToClaimFor].map(Json.toJson(_))";\
    print "      } yield (page, value)";\
    print "    }";\
    next }1' ../test/generators/UserAnswersEntryGenerators.scala > tmp && mv tmp ../test/generators/UserAnswersEntryGenerators.scala

echo "Adding to PageGenerators"
awk '/trait PageGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitrarySelectTaxYearsToClaimForPage: Arbitrary[SelectTaxYearsToClaimForPage.type] =";\
    print "    Arbitrary(SelectTaxYearsToClaimForPage)";\
    next }1' ../test/generators/PageGenerators.scala > tmp && mv tmp ../test/generators/PageGenerators.scala

echo "Adding to ModelGenerators"
awk '/trait ModelGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitrarySelectTaxYearsToClaimFor: Arbitrary[SelectTaxYearsToClaimFor] =";\
    print "    Arbitrary {";\
    print "      Gen.oneOf(SelectTaxYearsToClaimFor.values.toSeq)";\
    print "    }";\
    next }1' ../test/generators/ModelGenerators.scala > tmp && mv tmp ../test/generators/ModelGenerators.scala

echo "Adding to UserAnswersGenerator"
awk '/val generators/ {\
    print;\
    print "    arbitrary[(SelectTaxYearsToClaimForPage.type, JsValue)] ::";\
    next }1' ../test/generators/UserAnswersGenerator.scala > tmp && mv tmp ../test/generators/UserAnswersGenerator.scala

echo "Adding helper method to CheckYourAnswersHelper"
awk '/class/ {\
     print;\
     print "";\
     print "  def selectTaxYearsToClaimFor: Option[AnswerRow] = userAnswers.get(SelectTaxYearsToClaimForPage) map {";\
     print "    x =>";\
     print "      AnswerRow(";\
     print "        HtmlFormat.escape(messages(\"selectTaxYearsToClaimFor.checkYourAnswersLabel\")),";\
     print "        Html(x.map(value => HtmlFormat.escape(messages(s\"selectTaxYearsToClaimFor.$value\")).toString).mkString(\",<br>\")),";\
     print "        routes.SelectTaxYearsToClaimForController.onPageLoad(CheckMode).url";\
     print "      )"
     print "  }";\
     next }1' ../app/utils/CheckYourAnswersHelper.scala > tmp && mv tmp ../app/utils/CheckYourAnswersHelper.scala

echo "Migration SelectTaxYearsToClaimFor completed"
