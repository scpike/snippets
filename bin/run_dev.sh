#!/usr/bin/env bash

export DATABASE_URL="jdbc:postgresql://localhost:5432/snippets?user=snippets&password=snippets" 
lein run server-headless
