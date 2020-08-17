#!/bin/bash

echo ""
echo "Applying migration Disclaimer"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /disclaimer                       controllers.DisclaimerController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "disclaimer.title = disclaimer" >> ../conf/messages.en
echo "disclaimer.heading = disclaimer" >> ../conf/messages.en

echo "Migration Disclaimer completed"
