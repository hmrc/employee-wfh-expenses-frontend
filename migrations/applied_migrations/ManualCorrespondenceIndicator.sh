#!/bin/bash

echo ""
echo "Applying migration ManualCorrespondenceIndicator"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /manualCorrespondenceIndicator                       controllers.ManualCorrespondenceIndicatorController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "manualCorrespondenceIndicator.title = manualCorrespondenceIndicator" >> ../conf/messages.en
echo "manualCorrespondenceIndicator.heading = manualCorrespondenceIndicator" >> ../conf/messages.en

echo "Migration ManualCorrespondenceIndicator completed"
