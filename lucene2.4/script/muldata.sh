#!/bin/bash

dataset=(GOV2 WT10GT451-550);



for f in ${dataset[@]}; do
  #$1 $f ;
  export ETC="$f";$1
done
