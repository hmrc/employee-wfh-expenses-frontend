#!/bin/bash
sbt clean scalafmt test:scalafmt it/scalafmt coverage test it/test coverageReport