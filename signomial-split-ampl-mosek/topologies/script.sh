#!/bin/bash

cat $1  | awk '$1~/[0-9]/{if($1!=$2)print $1 "->" $2}'
