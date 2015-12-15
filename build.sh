#!/bin/sh

mvn clean install assembly:assembly -DdescriptorId=jar-with-dependencies

mv target/clobcopy-1.0-jar-with-dependencies.jar clobcopy/
