#!/bin/sh

OUT_FILE=`basename $1 .dot`.png
rm $OUT_FILE
dot -Gdpi=300 -Tpng -o $OUT_FILE $@
open $OUT_FILE