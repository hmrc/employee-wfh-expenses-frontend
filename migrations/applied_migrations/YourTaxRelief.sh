#!/bin/bash

echo ""
echo "Applying migration YourTaxRelief"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /yourTaxRelief                       controllers.YourTaxReliefController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "yourTaxRelief.title = yourTaxRelief" >> ../conf/messages.en
echo "yourTaxRelief.heading = yourTaxRelief" >> ../conf/messages.en

echo "Migration YourTaxRelief completed"
