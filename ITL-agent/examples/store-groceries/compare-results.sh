#collect goals used by LLM
grep "LM sourced goal:" cic.log | grep -v "Auto" > goal.log

#retrieved goals
echo "data type;object handle;response;prompt type" > retrieved.txt
grep "retrieved_response;" cic.log >> retrieved.txt

#offered goals
echo "data type;object handle;response;prompt type;human reply" > offered.txt
grep "offered_response;" cic.log >> offered.txt

#viable goals
echo "data_type;object handle;response;prompt type" > viable.txt
grep "viable_response;" cic.log | grep -v "unviable" >> viable.txt

#unviable goals
echo "data type;object handle;response;prompt type;mismatch type;mismatch;detail" > unviable.txt
grep "unviable_response;" cic.log >> unviable.txt


#sourced goals
echo "data type;object handle;response;prompt type" > used_response.txt
grep "used_response;" cic.log >> used_response.txt

#tokens
echo "prompt type;object handle;prompt tokens;completion tokens;total tokens" > tokens.txt
grep "prompt;" prompt.log >> tokens.txt
