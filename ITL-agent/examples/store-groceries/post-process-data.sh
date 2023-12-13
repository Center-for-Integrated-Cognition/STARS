sh copy-results.sh $1
cd data/$1/
sh compare-results.sh
sh replace_all_handles.sh
