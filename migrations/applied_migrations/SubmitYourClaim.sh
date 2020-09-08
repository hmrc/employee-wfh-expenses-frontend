#!/bin/bash

echo ""
echo "Applying migration SubmitYourClaim"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /submitYourClaim                       controllers.SubmitYourClaimController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "submitYourClaim.title = submitYourClaim" >> ../conf/messages.en
echo "submitYourClaim.heading = submitYourClaim" >> ../conf/messages.en

echo "Migration SubmitYourClaim completed"
