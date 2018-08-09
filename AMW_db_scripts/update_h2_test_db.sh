#!/usr/bin/env bash

mvn compile -projects AMW_db_scripts -Pliquibase,h2 -Ddb=h2.test -Dgoal=update
