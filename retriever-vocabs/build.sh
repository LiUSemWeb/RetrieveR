#!/bin/bash

### This scripts creates the relevant Java classes for the vocabularies. To use
### this script you need to set the JENA_HOME environment variable (pointing to
### the root directory of an unzipped Jena binary package) and then uncomment
### the relevant line at the very end of this script. 

# Check that JENA_HOME is not empty
if [ -z "$JENA_HOME" ]; then
  echo "JENA_HOME is not set"
  exit
fi

function syntaxCheck
{
    FILE="$1"
    echo "Syntax check: $FILE"
    echo ${JENA_HOME}/bin/riot
    ${JENA_HOME}/bin/riot --validate --sink "$FILE"
}

function proc
{
    TMP=TT
    FILE="$1"
    shift
    CLASS="$1"
    shift
    NS="$1"
    shift
    PKG="$1"
    shift
    DIR="$1"
    shift
    echo "Schemagen: $FILE"

    # -e syntax
    ${JENA_HOME}/bin/schemagen --owl -e TURTLE -i "$FILE" -n "$CLASS" -a "$NS" -o "$CLASS".java  "$@" 
    # Add imports
    echo "package ${PKG};" >> "$TMP"
    echo >>"$TMP"
    cat "$CLASS".java >> "$TMP"
    mv "$TMP" "$CLASS".java
    mv "$CLASS".java ${DIR}
}

function procVoCaLS
{
    syntaxCheck  vocals.ttl
    proc vocals.ttl \
         Vocals \
         "http://w3id.org/rsp/vocals#" \
         "se.liu.ida.retriever.vocabulary" \
         ./src/main/java/se/liu/ida/retriever/vocabulary/
}

### Below, uncomment the line for which you want to run the script.

procVoCaLS